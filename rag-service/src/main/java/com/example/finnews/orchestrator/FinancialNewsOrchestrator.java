package com.example.finnews.orchestrator;

import com.example.finnews.agent.AnalysisAgent;
import com.example.finnews.agent.MarketDataAgent;
import com.example.finnews.agent.NewsAgent;
import com.example.finnews.model.AnalysisRequest;
import com.example.finnews.model.AnalysisResponse;
import com.example.finnews.model.AuditEvent;
import com.example.finnews.model.KnowledgeChunk;
import com.example.finnews.rag.RagService;
import com.example.finnews.service.AuditEventRepository;
import com.example.finnews.service.InputPolicyService;
import com.example.finnews.service.RateLimitService;
import com.example.finnews.service.TelemetryService;
import io.micrometer.core.instrument.Timer.Sample;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class FinancialNewsOrchestrator {
    private final MarketDataAgent marketDataAgent;
    private final NewsAgent newsAgent;
    private final AnalysisAgent analysisAgent;
    private final RagService ragService;
    private final InputPolicyService inputPolicyService;
    private final RateLimitService rateLimitService;
    private final TelemetryService telemetryService;
    private final AuditEventRepository auditEventRepository;

    public AnalysisResponse run(AnalysisRequest request) {

        Sample timer = telemetryService.startTimer();
        List<String> toolCalls = new ArrayList<>();
        try {
            //Валидация роли
            inputPolicyService.validateRole(request.role());
            //Отслеживание кол-ва запросов от пользователя.
            rateLimitService.check(request.userId(), 10, 60);

            //Форматирование запроса пользователя, удаление нецензурной лексики, анонимизация ПДн, проверка на запрещенный контент.
            String sanitized = inputPolicyService.sanitize(request.userQuestion());
            String safeQuestion = inputPolicyService.anonymizePii(sanitized);
            inputPolicyService.validateContent(safeQuestion);

            List<KnowledgeChunk> docs = ragService.retrieve(request.symbol(), safeQuestion, 4);

            Map<String, Object> market = marketDataAgent.handle(request.symbol());
            toolCalls.add("get_stock_quote");
            toolCalls.add("get_daily_prices");

            Map<String, Object> news = newsAgent.handle(request.symbol());
            toolCalls.add("get_company_news");
            toolCalls.add("get_sec_filings");
            toolCalls.add("get_news_sentiment");

            String llmJson = analysisAgent.analyze(request.symbol(), safeQuestion, market, news, docs);
            List<String> citations = docs.stream().map(KnowledgeChunk::getSourceUrl).toList();

            auditEventRepository.save(AuditEvent.of(request.userId(), "analysis", "symbol=" + request.symbol()));
            telemetryService.success();

            return new AnalysisResponse(
                    llmJson,
                    "See JSON.recommendation",
                    0.75,
                    citations,
                    toolCalls,
                    List.of("Educational use only. Not investment advice.")
            );
        } catch (RuntimeException e) {
            telemetryService.error();
            auditEventRepository.save(AuditEvent.of(request.userId(), "error", e.getMessage()));
            throw e;
        } finally {
            telemetryService.stop(timer);
        }
    }
}
