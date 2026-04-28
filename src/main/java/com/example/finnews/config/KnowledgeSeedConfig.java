package com.example.finnews.config;

import com.example.finnews.model.KnowledgeChunk;
import com.example.finnews.rag.KnowledgeChunkRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KnowledgeSeedConfig {
    @Bean
    CommandLineRunner seedKnowledge(KnowledgeChunkRepository repository) {
        return args -> {
            if (repository.count() > 0) {
                return;
            }
            repository.save(build("GLOBAL", "Risk Management", "Use stop-loss and position sizing to reduce downside risk.", "https://example.com/risk"));
            repository.save(build("NVDA", "Semiconductor Cycles", "Chip demand can remain strong but valuation risk increases after rallies.", "https://example.com/nvda-cycle"));
            repository.save(build("AAPL", "Earnings Quality", "Services growth stability can offset hardware cyclicality.", "https://example.com/aapl-quality"));
        };
    }

    private KnowledgeChunk build(String ticker, String title, String content, String source) {
        KnowledgeChunk chunk = new KnowledgeChunk();
        chunk.setTicker(ticker);
        chunk.setTitle(title);
        chunk.setContent(content);
        chunk.setSourceUrl(source);
        return chunk;
    }
}
