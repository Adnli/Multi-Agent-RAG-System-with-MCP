package com.example.finnews.service;

import com.example.finnews.agent.AnalysisAgent;
import com.example.finnews.agent.MarketDataAgent;
import com.example.finnews.agent.NewsAgent;
import com.example.finnews.model.AnalysisRequest;
import com.example.finnews.model.AnalysisResult;
import com.example.finnews.obs.AuditLogger;
import com.example.finnews.obs.TelemetryCollector;
import com.example.finnews.obs.TelemetryEvent;
import com.example.finnews.security.AccessController;
import com.example.finnews.security.ContentFilter;
import com.example.finnews.security.InputSanitizer;
import com.example.finnews.security.PiiDetector;
import com.example.finnews.security.RateLimiter;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public class FinancialNewsOrchestrator {
    private final MarketDataAgent marketDataAgent;
    private final NewsAgent newsAgent;
    private final AnalysisAgent analysisAgent;
    private final TelemetryCollector telemetry;
    private final AuditLogger audit;
    private final InputSanitizer sanitizer;
    private final ContentFilter contentFilter;
    private final PiiDetector piiDetector;
    private final AccessController accessController;
    private final RateLimiter rateLimiter;

    public FinancialNewsOrchestrator(
            MarketDataAgent marketDataAgent,
            NewsAgent newsAgent,
            AnalysisAgent analysisAgent,
            TelemetryCollector telemetry,
            AuditLogger audit,
            InputSanitizer sanitizer,
            ContentFilter contentFilter,
            PiiDetector piiDetector,
            AccessController accessController,
            RateLimiter rateLimiter
    ) {
        this.marketDataAgent = marketDataAgent;
        this.newsAgent = newsAgent;
        this.analysisAgent = analysisAgent;
        this.telemetry = telemetry;
        this.audit = audit;
        this.sanitizer = sanitizer;
        this.contentFilter = contentFilter;
        this.piiDetector = piiDetector;
        this.accessController = accessController;
        this.rateLimiter = rateLimiter;
    }

    public AnalysisResult run(AnalysisRequest request) {
        long t0 = System.currentTimeMillis();
        String sanitized = sanitizer.sanitize(request.userQuestion());
        String anonymized = piiDetector.anonymize(sanitized);

        if (!accessController.isAuthorized(request.role())) {
            throw new SecurityException("Role is not authorized.");
        }
        if (!rateLimiter.allow(request.userId())) {
            throw new IllegalStateException("Rate limit exceeded.");
        }
        if (!contentFilter.isAllowed(anonymized)) {
            throw new IllegalArgumentException("Blocked by content policy.");
        }

        audit.log("request:user=" + request.userId() + ",symbol=" + request.symbol());

        Map<String, Object> market = timeAgent("market-agent", () -> marketDataAgent.handle(request.symbol()));
        Map<String, Object> news = timeAgent("news-agent", () -> newsAgent.handle(request.symbol()));
        String analysisJson = timeAgent("analysis-agent",
                () -> analysisAgent.analyze(request.symbol(), anonymized, market, news, List.of()));

        telemetry.record(new TelemetryEvent(
                "orchestrator",
                "end-to-end",
                System.currentTimeMillis() - t0,
                anonymized.length() / 4 + 120,
                true,
                Instant.now()
        ));
        audit.log("response:analysis=" + analysisJson);
        return new AnalysisResult(
                analysisJson,
                "See JSON.recommendation",
                0.75,
                false,
                List.of("https://example.org/rates", "https://example.org/earnings", "https://example.org/risk"),
                List.of("Educational use only. Not investment advice.")
        );
    }

    private <T> T timeAgent(String agent, AgentCall<T> call) {
        long started = System.currentTimeMillis();
        try {
            T value = call.exec();
            telemetry.record(new TelemetryEvent(agent, "handle", System.currentTimeMillis() - started, 80, true, Instant.now()));
            return value;
        } catch (RuntimeException ex) {
            telemetry.record(new TelemetryEvent(agent, "handle", System.currentTimeMillis() - started, 80, false, Instant.now()));
            throw ex;
        }
    }

    @FunctionalInterface
    private interface AgentCall<T> {
        T exec();
    }
}
