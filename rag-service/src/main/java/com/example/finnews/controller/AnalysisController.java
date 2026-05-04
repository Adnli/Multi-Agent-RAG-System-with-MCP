package com.example.finnews.controller;

import com.example.finnews.model.AnalysisRequest;
import com.example.finnews.model.AnalysisResponse;
import com.example.finnews.orchestrator.FinancialNewsOrchestrator;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/analysis")
@RequiredArgsConstructor
public class AnalysisController {
    private final FinancialNewsOrchestrator orchestrator;

    @PostMapping
    public AnalysisResponse analyze(@Valid @RequestBody AnalysisRequest request) {
        return orchestrator.run(request);
    }
}
