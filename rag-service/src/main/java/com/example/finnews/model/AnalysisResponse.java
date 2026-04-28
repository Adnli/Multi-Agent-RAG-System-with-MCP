package com.example.finnews.model;

import java.util.List;

public record AnalysisResponse(
        String summary,
        String recommendation,
        double confidence,
        List<String> citations,
        List<String> toolCalls,
        List<String> warnings
) {}
