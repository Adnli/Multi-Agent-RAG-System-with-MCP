package com.example.finnews.agent;

import com.example.finnews.mcp.FinancialMcpClient;
import com.example.finnews.model.CompanyProfile;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NewsAgentTest {

    @Test
    void buildsSearchQueryWithTickerAndCompanyName() {
        FinancialMcpClient mcp = mock(FinancialMcpClient.class);
        when(mcp.callTool(eq("search_engine"), eq("V"), org.mockito.ArgumentMatchers.anyMap()))
                .thenReturn(Map.of("ok", true));

        new NewsAgent(mcp).handle(new CompanyProfile("V", "Visa"));

        ArgumentCaptor<Map<String, Object>> argsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(mcp).callTool(eq("search_engine"), eq("V"), argsCaptor.capture());
        assertThat(argsCaptor.getValue())
                .containsEntry("engine", "google")
                .extractingByKey("query")
                .asString()
                .contains("V Visa")
                .contains("latest financial news");
    }
}
