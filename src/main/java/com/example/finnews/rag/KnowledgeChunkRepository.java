package com.example.finnews.rag;

import com.example.finnews.model.KnowledgeChunk;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface KnowledgeChunkRepository extends JpaRepository<KnowledgeChunk, Long> {
    List<KnowledgeChunk> findTop10ByTickerIgnoreCaseOrTickerIgnoreCase(String ticker, String globalTicker);
}
