package com.example.finnews.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class KnowledgeChunk {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String ticker;
    private String companyName;
    private String title;
    private String sourceType;
    private String sourceName;
    private String mcpToolName;
    private String dataProvider;
    private String queryText;

    @Column(length = 5000)
    private String content;

    @Column(length = 1200)
    private String sourceUrl;

    @Column(unique = true, length = 128)
    private String contentKey;

    private OffsetDateTime createdAt;
}
