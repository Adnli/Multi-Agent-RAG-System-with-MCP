package com.example.finnews.agent;

import com.example.finnews.mcp.FinancialMcpClient;
import com.example.finnews.model.CompanyProfile;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RiskAgentTest {

    @Test
    void buildsRiskBatchQueriesWithTickerAndCompanyName() {
        FinancialMcpClient mcp = mock(FinancialMcpClient.class);
        when(mcp.callTool(eq("search_engine_batch"), eq("AMZN"), org.mockito.ArgumentMatchers.anyMap()))
                .thenReturn(Map.of("ok", true));

        new RiskAgent(mcp).handle(new CompanyProfile("AMZN", "Amazon"));

        ArgumentCaptor<Map<String, Object>> argsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(mcp).callTool(eq("search_engine_batch"), eq("AMZN"), argsCaptor.capture());

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> queries = (List<Map<String, Object>>) argsCaptor.getValue().get("queries");

        assertThat(queries).hasSize(4);
        assertThat(queries)
                .extracting(query -> query.get("query").toString())
                .allMatch(query -> query.contains("AMZN Amazon"))
                .anyMatch(query -> query.contains("risk factors"))
                .anyMatch(query -> query.contains("lawsuit"))
                .anyMatch(query -> query.contains("downgrade"))
                .anyMatch(query -> query.contains("supply chain"));
    }
}
