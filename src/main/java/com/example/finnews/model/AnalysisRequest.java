package com.example.finnews.model;

import jakarta.validation.constraints.NotBlank;

public record AnalysisRequest(
        @NotBlank String userId,
        @NotBlank String role,
        @NotBlank String ticker,
        @NotBlank String question
) {}
