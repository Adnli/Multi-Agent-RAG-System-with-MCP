package com.example.finnews.service;

import com.example.finnews.model.KnowledgeChunk;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface KnowledgeChunkRepository extends JpaRepository<KnowledgeChunk, Long> {
    List<KnowledgeChunk> findTop50ByTickerOrderByIdDesc(String ticker);
    List<KnowledgeChunk> findTop200ByTickerOrderByIdDesc(String ticker);
    boolean existsByContentKey(String contentKey);
    Optional<KnowledgeChunk> findByContentKey(String contentKey);
}
