package com.example.finnews.rag;

import com.example.finnews.model.KnowledgeChunk;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class RagService {
    private final KnowledgeChunkRepository repository;

    public RagService(KnowledgeChunkRepository repository) {
        this.repository = repository;
    }

    public List<KnowledgeChunk> retrieve(String ticker, String question, int topK) {
        Set<String> qTerms = tokenize(question + " " + ticker);
        return repository.findTop10ByTickerIgnoreCaseOrTickerIgnoreCase(ticker, "GLOBAL")
                .stream()
                .sorted(Comparator.comparingDouble((KnowledgeChunk c) -> overlap(qTerms, tokenize(c.getContent()))).reversed())
                .limit(topK)
                .toList();
    }

    private double overlap(Set<String> q, Set<String> d) {
        if (q.isEmpty() || d.isEmpty()) {
            return 0;
        }
        long inter = q.stream().filter(d::contains).count();
        return (double) inter / Math.sqrt((double) q.size() * d.size());
    }

    private Set<String> tokenize(String text) {
        return List.of(text.toLowerCase().split("\\W+"))
                .stream()
                .filter(s -> !s.isBlank())
                .collect(Collectors.toSet());
    }
}
