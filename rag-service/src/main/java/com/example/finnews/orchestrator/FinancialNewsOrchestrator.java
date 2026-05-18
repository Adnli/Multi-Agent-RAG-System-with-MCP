package com.example.finnews.orchestrator;

import com.example.finnews.agent.AnalysisAgent;
import com.example.finnews.agent.MarketDataAgent;
import com.example.finnews.agent.NewsAgent;
import com.example.finnews.agent.RiskAgent;
import com.example.finnews.model.AnalysisRequest;
import com.example.finnews.model.AnalysisResponse;
import com.example.finnews.model.AuditEvent;
import com.example.finnews.service.AuditEventRepository;
import com.example.finnews.service.CompanyProfileResolver;
import com.example.finnews.service.InputPolicyService;
import com.example.finnews.service.RateLimitService;
import com.example.finnews.service.TelemetryService;
import com.example.finnews.service.RagKnowledgeService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Timer.Sample;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class FinancialNewsOrchestrator {
    private final MarketDataAgent marketDataAgent;
    private final NewsAgent newsAgent;
    private final RiskAgent riskAgent;
    private final AnalysisAgent analysisAgent;
    private final InputPolicyService inputPolicyService;
    private final RateLimitService rateLimitService;
    private final TelemetryService telemetryService;
    private final AuditEventRepository auditEventRepository;
    private final ObjectMapper objectMapper;
    private final RagKnowledgeService ragKnowledgeService;
    private final CompanyProfileResolver companyProfileResolver;

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
            var company = companyProfileResolver.resolve(request.symbol());

            var retrievedChunks = ragKnowledgeService.retrieveTopChunks(company, safeQuestion, 5);
            log.info("Retrieved {} RAG chunks for {}", retrievedChunks.size(), company.displayName());

            log.info("Market data request: {}", company.displayName());
            Map<String, Object> market = marketDataAgent.handle(company);
            toolCalls.add("web_data_yahoo_finance_business");
            log.info("Market data retrieved for {}: {}", company.displayName(), market);

            log.info("News request: {}", company.displayName());
            Map<String, Object> news = newsAgent.handle(company);
            toolCalls.add("search_engine");
            log.info("News retrieved for {}: {}", company.displayName(), news);
            int storedNewsChunks = ragKnowledgeService.ingestSearchResults(
                    company, news, "brightdata-mcp", "search_engine");
            log.info("Stored {} new news chunks for future RAG retrieval", storedNewsChunks);

            log.info("Risk request: {}", company.displayName());
            Map<String, Object> risk = riskAgent.handle(company);
            toolCalls.add("search_engine_batch");
            log.info("Risk retrieved for {}: {}", company.displayName(), risk);
            int storedRiskChunks = ragKnowledgeService.ingestSearchResults(
                    company, risk, "brightdata-mcp", "search_engine_batch");
            log.info("Stored {} new risk chunks for future RAG retrieval", storedRiskChunks);

            String llm = analysisAgent.analyze(company, safeQuestion, market, news, risk, retrievedChunks);
            log.info("result of llm:{}", llm);
            Map<String, Object> llmJson = objectMapper.readValue(llm, new TypeReference<>() {});

            auditEventRepository.save(AuditEvent.of(request.userId(), "analysis", "symbol=" + company.ticker()));
            telemetryService.success();

            return new AnalysisResponse(
                    (String) llmJson.get("summary"),
                    (String) llmJson.get("recommendation"),
                    (double) llmJson.get("confidence"),
                    objectMapper.convertValue(llmJson.get("citations"), new TypeReference<>() {}),
                    toolCalls,
                    objectMapper.convertValue(llmJson.get("warnings"), new TypeReference<>() {}));
        } catch (RuntimeException e) {
            telemetryService.error();
            auditEventRepository.save(AuditEvent.of(request.userId(), "error", e.getMessage()));
            throw e;
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        } finally {
            telemetryService.stop(timer);
        }
    }
}
