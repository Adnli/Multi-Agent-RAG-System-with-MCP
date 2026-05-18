package com.example.finnews.orchestrator;

import com.example.finnews.agent.AnalysisAgent;
import com.example.finnews.agent.MarketDataAgent;
import com.example.finnews.agent.NewsAgent;
import com.example.finnews.agent.RiskAgent;
import com.example.finnews.model.AnalysisRequest;
import com.example.finnews.model.AnalysisResponse;
import com.example.finnews.model.AuditEvent;
import com.example.finnews.model.CompanyProfile;
import com.example.finnews.service.AuditEventRepository;
import com.example.finnews.service.CompanyProfileResolver;
import com.example.finnews.service.InputPolicyService;
import com.example.finnews.service.RagKnowledgeService;
import com.example.finnews.service.RateLimitService;
import com.example.finnews.service.TelemetryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FinancialNewsOrchestratorTest {

    @Test
    void runsProductionFlowWithCompanyProfileAndRagBeforeFreshIngest() {
        MarketDataAgent marketDataAgent = mock(MarketDataAgent.class);
        NewsAgent newsAgent = mock(NewsAgent.class);
        RiskAgent riskAgent = mock(RiskAgent.class);
        AnalysisAgent analysisAgent = mock(AnalysisAgent.class);
        RagKnowledgeService ragKnowledgeService = mock(RagKnowledgeService.class);
        AuditEventRepository auditEventRepository = mock(AuditEventRepository.class);
        TelemetryService telemetryService = new TelemetryService(new SimpleMeterRegistry());

        CompanyProfile company = new CompanyProfile("V", "Visa");
        when(marketDataAgent.handle(company)).thenReturn(Map.of("market", "ok"));
        when(newsAgent.handle(company)).thenReturn(Map.of("news", "ok"));
        when(riskAgent.handle(company)).thenReturn(Map.of("risk", "ok"));
        when(ragKnowledgeService.retrieveTopChunks(eq(company), eq("Analyze Visa risks"), eq(5)))
                .thenReturn(List.of());
        when(ragKnowledgeService.ingestSearchResults(eq(company), any(), eq("brightdata-mcp"), eq("search_engine")))
                .thenReturn(1);
        when(ragKnowledgeService.ingestSearchResults(eq(company), any(), eq("brightdata-mcp"), eq("search_engine_batch")))
                .thenReturn(2);
        when(analysisAgent.analyze(eq(company), eq("Analyze Visa risks"), any(), any(), any(), eq(List.of())))
                .thenReturn("""
                        {"summary":"Visa summary","recommendation":"Hold","confidence":0.7,"citations":[],"warnings":["Educational use only."]}
                        """);

        FinancialNewsOrchestrator orchestrator = new FinancialNewsOrchestrator(
                marketDataAgent,
                newsAgent,
                riskAgent,
                analysisAgent,
                new InputPolicyService(),
                new RateLimitService(),
                telemetryService,
                auditEventRepository,
                new ObjectMapper(),
                ragKnowledgeService,
                new CompanyProfileResolver()
        );

        AnalysisResponse response = orchestrator.run(
                new AnalysisRequest("user-1", "analyst", "v", "Analyze Visa risks"));

        assertThat(response.summary()).isEqualTo("Visa summary");
        assertThat(response.recommendation()).isEqualTo("Hold");
        assertThat(response.confidence()).isEqualTo(0.7);
        assertThat(response.toolCalls()).containsExactly(
                "web_data_yahoo_finance_business",
                "search_engine",
                "search_engine_batch"
        );

        InOrder order = inOrder(ragKnowledgeService, marketDataAgent, newsAgent, riskAgent, analysisAgent);
        order.verify(ragKnowledgeService).retrieveTopChunks(eq(company), eq("Analyze Visa risks"), eq(5));
        order.verify(marketDataAgent).handle(company);
        order.verify(newsAgent).handle(company);
        order.verify(ragKnowledgeService).ingestSearchResults(eq(company), any(), eq("brightdata-mcp"), eq("search_engine"));
        order.verify(riskAgent).handle(company);
        order.verify(ragKnowledgeService).ingestSearchResults(eq(company), any(), eq("brightdata-mcp"), eq("search_engine_batch"));
        order.verify(analysisAgent).analyze(eq(company), eq("Analyze Visa risks"), any(), any(), any(), eq(List.of()));

        ArgumentCaptor<AuditEvent> auditCaptor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditEventRepository).save(auditCaptor.capture());
        assertThat(auditCaptor.getValue().getUserId()).isEqualTo("user-1");
        assertThat(auditCaptor.getValue().getAction()).isEqualTo("analysis");
        assertThat(auditCaptor.getValue().getDetail()).isEqualTo("symbol=V");
    }
}
