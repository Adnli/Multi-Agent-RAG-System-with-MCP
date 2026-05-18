package com.example.finnews.service;

import com.example.finnews.model.CompanyProfile;
import com.example.finnews.model.KnowledgeChunk;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RagKnowledgeServiceTest {

    private KnowledgeChunkRepository repository;
    private RagKnowledgeService service;

    @BeforeEach
    void setUp() {
        repository = mock(KnowledgeChunkRepository.class);
        service = new RagKnowledgeService(repository, new ObjectMapper());
    }

    @Test
    void ingestsSearchRowsWithMcpProvenanceAndCompanyContext() {
        when(repository.findByContentKey(anyString())).thenReturn(Optional.empty());
        CompanyProfile company = new CompanyProfile("V", "Visa");
        Map<String, Object> payload = Map.of(
                "search", Map.of(
                        "query", "V Visa latest financial news earnings guidance stock",
                        "engine", "google",
                        "organic", List.of(Map.of(
                                "title", "Visa reports earnings growth",
                                "description", "Visa earnings guidance improved after resilient payment volume.",
                                "link", "https://example.test/visa-earnings"
                        ))
                )
        );

        int stored = service.ingestSearchResults(company, payload, "brightdata-mcp", "search_engine");

        ArgumentCaptor<KnowledgeChunk> chunkCaptor = ArgumentCaptor.forClass(KnowledgeChunk.class);
        verify(repository).save(chunkCaptor.capture());

        KnowledgeChunk chunk = chunkCaptor.getValue();
        assertThat(stored).isEqualTo(1);
        assertThat(chunk.getTicker()).isEqualTo("V");
        assertThat(chunk.getCompanyName()).isEqualTo("Visa");
        assertThat(chunk.getTitle()).isEqualTo("Visa reports earnings growth");
        assertThat(chunk.getContent()).contains("payment volume");
        assertThat(chunk.getSourceUrl()).isEqualTo("https://example.test/visa-earnings");
        assertThat(chunk.getSourceType()).isEqualTo("mcp_tool_result");
        assertThat(chunk.getSourceName()).isEqualTo("brightdata-mcp");
        assertThat(chunk.getMcpToolName()).isEqualTo("search_engine");
        assertThat(chunk.getDataProvider()).isEqualTo("google");
        assertThat(chunk.getQueryText()).contains("V Visa");
        assertThat(chunk.getContentKey()).isNotBlank();
        assertThat(chunk.getCreatedAt()).isNotNull();
    }

    @Test
    void updatesExistingChunkInsteadOfCreatingDuplicateForSameSourceUrl() {
        KnowledgeChunk existing = new KnowledgeChunk();
        existing.setTicker("AAPL");
        existing.setCompanyName("Apple");
        existing.setSourceUrl("https://example.test/apple");
        existing.setContentKey("existing-key");

        when(repository.findByContentKey(anyString())).thenReturn(Optional.of(existing));

        int stored = service.ingestSearchResults(
                new CompanyProfile("AAPL", "Apple"),
                Map.of("results", List.of(Map.of(
                        "headline", "Apple supplier risk update",
                        "summary", "Apple faces renewed supply chain risk.",
                        "url", "https://example.test/apple",
                        "provider", "google"
                ))),
                "brightdata-mcp",
                "search_engine_batch"
        );

        assertThat(stored).isZero();
        assertThat(existing.getTitle()).isEqualTo("Apple supplier risk update");
        assertThat(existing.getContent()).contains("supply chain risk");
        assertThat(existing.getSourceName()).isEqualTo("brightdata-mcp");
        assertThat(existing.getMcpToolName()).isEqualTo("search_engine_batch");
        assertThat(existing.getDataProvider()).isEqualTo("google");
        verify(repository).save(existing);
    }

    @Test
    void parsesJsonStringPayloadAndAlternativeResultKeys() {
        when(repository.findByContentKey(anyString())).thenReturn(Optional.empty());
        String jsonPayload = """
                {
                  "query": "AMZN Amazon lawsuit regulatory investigation",
                  "provider": "google",
                  "items": [
                    {
                      "headline": "Amazon faces regulatory scrutiny",
                      "summary": "Amazon is under renewed antitrust review.",
                      "source_url": "https://example.test/amazon-antitrust"
                    }
                  ]
                }
                """;

        int stored = service.ingestSearchResults(
                new CompanyProfile("AMZN", "Amazon"),
                Map.of("wrapped", jsonPayload),
                "brightdata-mcp",
                "search_engine_batch"
        );

        ArgumentCaptor<KnowledgeChunk> chunkCaptor = ArgumentCaptor.forClass(KnowledgeChunk.class);
        verify(repository).save(chunkCaptor.capture());

        assertThat(stored).isEqualTo(1);
        assertThat(chunkCaptor.getValue().getTitle()).isEqualTo("Amazon faces regulatory scrutiny");
        assertThat(chunkCaptor.getValue().getSourceUrl()).isEqualTo("https://example.test/amazon-antitrust");
        assertThat(chunkCaptor.getValue().getDataProvider()).isEqualTo("google");
    }

    @Test
    void ranksLexicalMatchesBeforeRecentFallback() {
        KnowledgeChunk olderRelevant = chunk(10L, "MCD", "McDonald's", "McDonald's lawsuit update",
                "Regulatory investigation and lawsuit risk increased.", "https://example.test/mcd-lawsuit");
        KnowledgeChunk newerIrrelevant = chunk(20L, "MCD", "McDonald's", "McDonald's menu launch",
                "New seasonal menu item launched.", "https://example.test/mcd-menu");

        when(repository.findTop200ByTickerOrderByIdDesc("MCD"))
                .thenReturn(List.of(newerIrrelevant, olderRelevant));

        List<KnowledgeChunk> chunks = service.retrieveTopChunks(
                new CompanyProfile("MCD", "McDonald's"),
                "What are the lawsuit risks?",
                1
        );

        assertThat(chunks).containsExactly(olderRelevant);
    }

    @Test
    void fallsBackToRecentTickerChunksWhenQuestionHasNoLexicalMatch() {
        KnowledgeChunk first = chunk(2L, "V", "Visa", "Visa earnings",
                "Payment volume grew.", "https://example.test/visa-earnings");
        KnowledgeChunk second = chunk(1L, "V", "Visa", "Visa guidance",
                "Management updated guidance.", "https://example.test/visa-guidance");

        when(repository.findTop200ByTickerOrderByIdDesc("V"))
                .thenReturn(List.of(first, second));

        List<KnowledgeChunk> chunks = service.retrieveTopChunks(
                new CompanyProfile("V", "Visa"),
                "Расскажи простыми словами",
                2
        );

        assertThat(chunks).containsExactly(first, second);
    }

    @Test
    void skipsInvalidRowsWithoutSaving() {
        int stored = service.ingestSearchResults(
                new CompanyProfile("AAPL", "Apple"),
                Map.of("organic", List.of(Map.of("title", "No URL", "description", "Missing link"))),
                "brightdata-mcp",
                "search_engine"
        );

        assertThat(stored).isZero();
        verify(repository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    private KnowledgeChunk chunk(Long id,
                                 String ticker,
                                 String companyName,
                                 String title,
                                 String content,
                                 String sourceUrl) {
        KnowledgeChunk chunk = new KnowledgeChunk();
        chunk.setId(id);
        chunk.setTicker(ticker);
        chunk.setCompanyName(companyName);
        chunk.setTitle(title);
        chunk.setContent(content);
        chunk.setSourceUrl(sourceUrl);
        return chunk;
    }
}
