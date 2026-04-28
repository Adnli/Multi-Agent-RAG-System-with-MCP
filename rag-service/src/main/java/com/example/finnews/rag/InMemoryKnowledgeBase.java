package com.example.finnews.rag;

import com.example.finnews.model.KnowledgeDocument;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

public class InMemoryKnowledgeBase {
    private final List<KnowledgeDocument> docs = new ArrayList<>();

    public InMemoryKnowledgeBase() {
        docs.add(new KnowledgeDocument("k1", "Rate Hike Pattern", "Historically, aggressive rate hikes increase volatility in growth stocks.", "https://example.org/rates"));
        docs.add(new KnowledgeDocument("k2", "Earnings Surprise", "Positive earnings surprises often lead to short-term momentum.", "https://example.org/earnings"));
        docs.add(new KnowledgeDocument("k3", "Risk Management", "Position sizing and stop-loss rules reduce drawdown risk.", "https://example.org/risk"));
    }

    public List<KnowledgeDocument> retrieve(String query, int topK) {
        Set<String> qTerms = tokenize(query);
        return docs.stream()
                .sorted(Comparator.comparingDouble((KnowledgeDocument d) -> overlapScore(qTerms, tokenize(d.body()))).reversed())
                .limit(topK)
                .collect(Collectors.toList());
    }

    private double overlapScore(Set<String> qTerms, Set<String> dTerms) {
        if (qTerms.isEmpty() || dTerms.isEmpty()) {
            return 0;
        }
        long overlap = qTerms.stream().filter(dTerms::contains).count();
        return (double) overlap / Math.sqrt((double) qTerms.size() * dTerms.size());
    }

    private Set<String> tokenize(String text) {
        return List.of(text.toLowerCase(Locale.ROOT).split("\\W+"))
                .stream()
                .filter(t -> !t.isBlank())
                .collect(Collectors.toSet());
    }
}
