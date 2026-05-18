package com.example.finnews.service;

import com.example.finnews.model.KnowledgeChunk;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RagKnowledgeService {

    private static final Pattern NON_ALPHANUM = Pattern.compile("[^\\p{L}\\p{N}]+", Pattern.UNICODE_CHARACTER_CLASS);
    private static final Set<String> STOPWORDS = Set.of(
            "the", "and", "for", "with", "that", "from", "this", "are", "was", "were", "have", "has",
            "will", "about", "into", "your", "their", "как", "что", "это", "для", "или", "при", "над",
            "under", "over", "после", "before", "risk", "news", "stock", "company", "market"
    );

    private final KnowledgeChunkRepository knowledgeChunkRepository;

    public void ingestSearchResults(String ticker, Map<String, Object> payload) {
        List<Map<String, String>> rows = extractOrganic(payload);
        for (Map<String, String> row : rows) {
            String title = row.getOrDefault("title", "");
            String description = row.getOrDefault("description", "");
            String link = row.getOrDefault("link", "");
            if (title.isBlank() || description.isBlank() || link.isBlank()) {
                continue;
            }
            KnowledgeChunk chunk = new KnowledgeChunk();
            chunk.setTicker(ticker);
            chunk.setTitle(truncate(title, 500));
            chunk.setContent(truncate(description, 4500));
            chunk.setSourceUrl(link);
            knowledgeChunkRepository.save(chunk);
        }
    }

    public List<KnowledgeChunk> retrieveTopChunks(String ticker, String question, int limit) {
        List<String> queryTokens = tokenize(question);
        if (queryTokens.isEmpty()) {
            return List.of();
        }

        return knowledgeChunkRepository.findTop50ByTickerOrderByIdDesc(ticker).stream()
                .map(chunk -> Map.entry(chunk, score(chunk, queryTokens)))
                .filter(entry -> entry.getValue() > 0)
                .sorted(Map.Entry.<KnowledgeChunk, Integer>comparingByValue(Comparator.reverseOrder())
                        .thenComparing(entry -> entry.getKey().getId(), Comparator.reverseOrder()))
                .limit(limit)
                .map(Map.Entry::getKey)
                .toList();
    }

    private int score(KnowledgeChunk chunk, List<String> queryTokens) {
        List<String> docTokens = tokenize(chunk.getTitle() + " " + chunk.getContent());
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
        List<String> tokens = new ArrayList<>();
        for (String token : cleaned.split("\\s+")) {
            if (token.length() < 3 || STOPWORDS.contains(token)) {
                continue;
            }
            tokens.add(token);
        }
        return tokens;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, String>> extractOrganic(Object node) {
        if (node == null) return List.of();
        if (node instanceof List<?> list) {
            return list.stream().map(this::extractOrganic).flatMap(List::stream).toList();
        }
        if (!(node instanceof Map<?, ?> map)) {
            return List.of();
        }

        Object organic = map.get("organic");
        if (organic instanceof List<?> organicList) {
            return organicList.stream()
                    .filter(Map.class::isInstance)
                    .map(item -> (Map<String, Object>) item)
                    .map(item -> Map.of(
                            "title", Objects.toString(item.get("title"), ""),
                            "description", Objects.toString(item.get("description"), ""),
                            "link", Objects.toString(item.get("link"), "")))
                    .collect(Collectors.toList());
        }

        return map.values().stream()
                .map(this::extractOrganic)
                .flatMap(List::stream)
                .toList();
    }

    private String truncate(String value, int max) {
        return value.length() <= max ? value : value.substring(0, max);
    }
}
