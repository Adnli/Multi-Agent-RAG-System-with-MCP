package com.example.finnews;

import com.example.finnews.agent.AnalysisAgent;
import com.example.finnews.agent.MarketDataAgent;
import com.example.finnews.agent.NewsAgent;
import com.example.finnews.mcp.MockMcpToolClient;
import com.example.finnews.model.AnalysisRequest;
import com.example.finnews.model.AnalysisResult;
import com.example.finnews.obs.AuditLogger;
import com.example.finnews.obs.TelemetryCollector;
import com.example.finnews.rag.InMemoryKnowledgeBase;
import com.example.finnews.security.AccessController;
import com.example.finnews.security.ContentFilter;
import com.example.finnews.security.InputSanitizer;
import com.example.finnews.security.PiiDetector;
import com.example.finnews.security.RateLimiter;
import com.example.finnews.service.FinancialNewsOrchestrator;

public class App {
    public static void main(String[] args) {
        var mcp = new MockMcpToolClient();
        var orchestrator = new FinancialNewsOrchestrator(
                new MarketDataAgent(mcp),
                new NewsAgent(mcp),
                new AnalysisAgent(new InMemoryKnowledgeBase()),
                new TelemetryCollector(),
                new AuditLogger(),
                new InputSanitizer(),
                new ContentFilter(),
                new PiiDetector(),
                new AccessController(),
                new RateLimiter(5, 60)
        );

        AnalysisRequest request = new AnalysisRequest("u1", "student", "NVDA", "Оцени риск и дай консервативную рекомендацию.");
        AnalysisResult result = orchestrator.run(request);

        System.out.println("Summary: " + result.summary());
        System.out.println("Recommendation: " + result.recommendation());
        System.out.println("Confidence: " + result.confidence());
        System.out.println("Citations: " + result.citations());
        System.out.println("Warnings: " + result.warnings());
    }
}
