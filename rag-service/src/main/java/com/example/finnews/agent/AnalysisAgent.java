package com.example.finnews.agent;

import com.example.finnews.model.KnowledgeChunk;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class AnalysisAgent {
    private final ChatClient chatClient;

    @Autowired
    public AnalysisAgent(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    public String analyze(String ticker,
                          String question,
                          Map<String, Object> marketData,
                          Map<String, Object> newsData,
                          Map<String, Object> risk,
                          java.util.List<KnowledgeChunk> ragContext) {
        String formattedRagContext = ragContext.stream()
                .map(chunk -> """
                        - title: %s
                          content: %s
                          source: %s
                          mcpServer: %s
                          mcpTool: %s
                          dataProvider: %s
                        """.formatted(
                        Objects.toString(chunk.getTitle(), ""),
                        Objects.toString(chunk.getContent(), ""),
                        Objects.toString(chunk.getSourceUrl(), ""),
                        Objects.toString(chunk.getSourceName(), ""),
                        Objects.toString(chunk.getMcpToolName(), ""),
                        Objects.toString(chunk.getDataProvider(), "")))
                .collect(Collectors.joining("\n"));

        return Objects.requireNonNull(chatClient.prompt()
                        .system("""
                You are a conservative financial analyst.
                Always mention uncertainty and risk.

                Citation rules:
                - Use ONLY URLs that appear verbatim in the provided input.
                - Never invent URLs.
                - Never use example.com or placeholder links.
                - If no reliable source URL is available, return an empty citations array.
                """)
                        .user(u -> u.text("""
                        Ticker: {ticker}
                        User question: {question}

                        Market data from MCP tools:
                        {market}

                        News data from MCP tools:
                        {news}

                        Risks data from MCP tools:
                        {risks}

                        Retrieved context from internal knowledge base (RAG):
                        {rag}

                        Prioritize facts supported by this RAG context when relevant.

                        Return clean valid JSON with these exact fields:
                        summary, recommendation, confidence, citations, warnings.

                        Field requirements:
                        summary: string.
                        recommendation: string.
                        confidence: number from 0.0 to 1.0.
                        citations: array of exact URLs copied from the input only.
                        warnings: array of strings.

                        Answer in the language in which the question was asked.
                        """)
                                .param("ticker", ticker)
                                .param("question", question)
                                .param("market", marketData.toString())
                                .param("news", newsData.toString())
                                .param("risks", risk.toString())
                                .param("rag", formattedRagContext))
                        .call()
                        .content())
                .trim()
                .replaceFirst("^```json\\s*", "")
                .replaceFirst("^```\\s*", "")
                .replaceFirst("\\s*```$", "")
                .trim();
    }
}
