package com.example.finnews.service;

import com.example.finnews.model.KnowledgeChunk;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class RagKnowledgeService {

    private static final Pattern NON_ALPHANUM = Pattern.compile("[^\\p{L}\\p{N}]+", Pattern.UNICODE_CHARACTER_CLASS);
    private static final Set<String> STOPWORDS = Set.of(
            "the", "and", "for", "with", "that", "from", "this", "are", "was", "were", "have", "has",
            "will", "about", "into", "your", "their", "как", "что", "это", "для", "или", "при", "над",
            "under", "over", "после", "before", "news", "stock", "company", "market"
    );

    private final KnowledgeChunkRepository knowledgeChunkRepository;
    private final ObjectMapper objectMapper;

    public int ingestSearchResults(String ticker, Map<String, Object> payload, String mcpServerName, String toolName) {
        log.info("RAG ingest from MCP server={}, tool={}, symbol={}, payload={}",
                mcpServerName, toolName, ticker, payload);
        String normalizedTicker = normalizeTicker(ticker);
        List<Map<String, String>> rows = extractOrganic(payload);
        log.info("Extracted {} source rows for {} from tool {}", rows.size(), ticker, toolName);
        int stored = 0;
        for (Map<String, String> row : rows) {
            String title = row.getOrDefault("title", "");
            String description = row.getOrDefault("description", "");
            String link = row.getOrDefault("link", "");
            if (title.isBlank() || description.isBlank() || link.isBlank()) {
                continue;
            }
            String contentKey = contentKey(normalizedTicker, link);
            KnowledgeChunk existingChunk = knowledgeChunkRepository.findByContentKey(contentKey).orElse(null);
            if (existingChunk != null) {
                refreshExistingChunk(existingChunk, row, title, description, link, mcpServerName, toolName);
                continue;
            }

            KnowledgeChunk chunk = new KnowledgeChunk();
            chunk.setTicker(normalizedTicker);
            chunk.setTitle(truncate(title, 500));
            chunk.setContent(truncate(description, 4500));
            chunk.setSourceUrl(link);
            chunk.setSourceType("mcp_tool_result");
            chunk.setSourceName(safeValue(mcpServerName));
            chunk.setMcpToolName(safeValue(toolName));
            chunk.setDataProvider(row.getOrDefault("engine", ""));
            chunk.setQueryText(truncate(row.getOrDefault("query", ""), 500));
            chunk.setContentKey(contentKey);
            chunk.setCreatedAt(OffsetDateTime.now());
            knowledgeChunkRepository.save(chunk);
            stored++;
        }
        return stored;
    }

    public List<KnowledgeChunk> retrieveTopChunks(String ticker, String question, int limit) {
        String normalizedTicker = normalizeTicker(ticker);
        List<String> queryTokens = tokenize(normalizedTicker + " " + Objects.toString(question, ""));
        if (normalizedTicker.isBlank()) {
            return List.of();
        }

        List<KnowledgeChunk> candidates = knowledgeChunkRepository.findTop200ByTickerOrderByIdDesc(normalizedTicker);
        if (candidates.isEmpty()) {
            return List.of();
        }
        if (queryTokens.isEmpty()) {
            return candidates.stream().limit(limit).toList();
        }

        List<KnowledgeChunk> ranked = candidates.stream()
                .map(chunk -> Map.entry(chunk, score(chunk, queryTokens)))
                .filter(entry -> entry.getValue() > 0)
                .sorted(Map.Entry.<KnowledgeChunk, Integer>comparingByValue(Comparator.reverseOrder())
                        .thenComparing(entry -> entry.getKey().getId(), Comparator.reverseOrder()))
                .limit(limit)
                .map(Map.Entry::getKey)
                .toList();

        if (!ranked.isEmpty()) {
            return ranked;
        }

        log.info("No lexical RAG match for symbol {} and question '{}'; falling back to recent chunks by ticker.",
                normalizedTicker, question);
        return candidates.stream().limit(limit).toList();
    }

    private int score(KnowledgeChunk chunk, List<String> queryTokens) {
        List<String> docTokens = tokenize(chunk.getTicker()
                + " " + chunk.getTitle()
                + " " + chunk.getContent()
                + " " + Objects.toString(chunk.getQueryText(), ""));
        Set<String> unique = Set.copyOf(docTokens);
        int score = 0;
        for (String token : queryTokens) {
            if (unique.contains(token)) {
                score++;
            }
        }
        return score;
    }

    private List<String> tokenize(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        String cleaned = NON_ALPHANUM.matcher(text.toLowerCase(Locale.ROOT)).replaceAll(" ");
        Set<String> tokens = new LinkedHashSet<>();
        for (String token : cleaned.split("\\s+")) {
            if (token.length() < 3 || STOPWORDS.contains(token)) {
                continue;
            }
            tokens.add(token);
        }
        return List.copyOf(tokens);
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, String>> extractOrganic(Object node) {
        return extractOrganic(node, "", "");
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, String>> extractOrganic(Object node, String currentQuery, String currentEngine) {
        if (node == null) {
            return List.of();
        }

        if (node instanceof String text) {
            return extractOrganicFromString(text, currentQuery, currentEngine);
        }

        if (node instanceof List<?> list) {
            return list.stream()
                    .map(item -> extractOrganic(item, currentQuery, currentEngine))
                    .flatMap(List::stream)
                    .toList();
        }

        if (!(node instanceof Map<?, ?> map)) {
            return List.of();
        }

        String nextQuery = Objects.toString(map.get("query"), currentQuery);
        String nextEngine = firstNonBlankWithFallback(map, currentEngine, "engine", "provider", "source", "sourceName");

        List<?> sourceRows = firstList(map, "organic", "results", "items", "news", "data");
        if (sourceRows != null) {
            return sourceRows.stream()
                    .filter(Map.class::isInstance)
                    .map(item -> (Map<String, Object>) item)
                    .map(item -> toSearchRow(item, nextQuery, nextEngine))
                    .filter(row -> !row.get("title").isBlank()
                            && !row.get("description").isBlank()
                            && !row.get("link").isBlank())
                    .toList();
        }

        return map.values().stream()
                .map(value -> extractOrganic(value, nextQuery, nextEngine))
                .flatMap(List::stream)
                .toList();
    }

    private List<Map<String, String>> extractOrganicFromString(String text, String currentQuery, String currentEngine) {
        String trimmed = stripCodeFence(text.trim());

        if (!(trimmed.startsWith("{") || trimmed.startsWith("["))) {
            return List.of();
        }

        try {
            Object parsed = objectMapper.readValue(trimmed, Object.class);
            return extractOrganic(parsed, currentQuery, currentEngine);
        } catch (Exception ex) {
            log.warn("Failed to parse search result text as JSON: {}", preview(trimmed), ex);
            return List.of();
        }
    }

    private Map<String, String> toSearchRow(Map<String, Object> item, String query, String engine) {
        String title = firstNonBlank(item, "title", "name", "headline");
        String description = firstNonBlank(item, "description", "snippet", "text", "content", "summary");
        String link = firstNonBlank(item, "link", "url", "sourceUrl", "source_url");
        String provider = firstNonBlankWithFallback(item, engine, "engine", "provider", "source", "sourceName");
        return Map.of(
                "title", title,
                "description", description,
                "link", link,
                "query", Objects.toString(query, ""),
                "engine", provider
        );
    }

    private String stripCodeFence(String value) {
        if (value.startsWith("```")) {
            return value
                    .replaceFirst("^```(?:json)?\\s*", "")
                    .replaceFirst("\\s*```$", "")
                    .trim();
        }
        return value;
    }

    private String preview(String value) {
        return value.length() <= 300 ? value : value.substring(0, 300) + "...";
    }

    private void refreshExistingChunk(KnowledgeChunk chunk,
                                      Map<String, String> row,
                                      String title,
                                      String description,
                                      String link,
                                      String mcpServerName,
                                      String toolName) {
        chunk.setTitle(truncate(title, 500));
        chunk.setContent(truncate(description, 4500));
        chunk.setSourceUrl(link);
        chunk.setSourceType("mcp_tool_result");
        chunk.setSourceName(safeValue(mcpServerName));
        chunk.setMcpToolName(safeValue(toolName));
        chunk.setDataProvider(row.getOrDefault("engine", ""));
        chunk.setQueryText(truncate(row.getOrDefault("query", ""), 500));
        chunk.setCreatedAt(OffsetDateTime.now());
        knowledgeChunkRepository.save(chunk);
    }

    private String firstNonBlank(Map<String, Object> item, String... keys) {
        for (String key : keys) {
            String value = Objects.toString(item.get(key), "").trim();
            if (!value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    private String firstNonBlankWithFallback(Map<?, ?> item, String fallback, String... keys) {
        for (String key : keys) {
            String value = Objects.toString(item.get(key), "").trim();
            if (!value.isBlank()) {
                return value;
            }
        }
        return Objects.toString(fallback, "");
    }

    private List<?> firstList(Map<?, ?> map, String... keys) {
        for (String key : keys) {
            Object value = map.get(key);
            if (value instanceof List<?> list) {
                return list;
            }
        }
        return null;
    }

    private String safeValue(String value) {
        return Objects.toString(value, "").trim();
    }

    private String contentKey(String ticker, String link) {
        String stableValue = normalizeTicker(ticker)
                + "|" + normalizeUrl(link);
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(stableValue.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 digest is not available.", e);
        }
    }

    private String normalizeWhitespace(String value) {
        return Objects.toString(value, "").trim().replaceAll("\\s+", " ");
    }

    private String normalizeUrl(String value) {
        String normalized = normalizeWhitespace(value).toLowerCase(Locale.ROOT);
        if (normalized.endsWith("/")) {
            return normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private String normalizeTicker(String ticker) {
        return Objects.toString(ticker, "").trim().toUpperCase(Locale.ROOT);
    }

    private String truncate(String value, int max) {
        return value.length() <= max ? value : value.substring(0, max);
    }
}
