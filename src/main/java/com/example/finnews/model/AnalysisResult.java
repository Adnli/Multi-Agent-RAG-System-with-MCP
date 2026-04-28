package com.example.finnews.model;

import java.util.List;

public record AnalysisResult(
        String summary,
        String recommendation,
        double confidence,
        boolean potentialHallucination,
        List<String> citations,
        List<String> warnings
) {}
