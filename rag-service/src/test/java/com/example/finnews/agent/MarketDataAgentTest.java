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

class MarketDataAgentTest {

    @Test
    void usesTickerForYahooFinanceUrlAndPassesCompanyName() {
        FinancialMcpClient mcp = mock(FinancialMcpClient.class);
        when(mcp.callTool(eq("web_data_yahoo_finance_business"), eq("MCD"), org.mockito.ArgumentMatchers.anyMap()))
                .thenReturn(Map.of("ok", true));

        new MarketDataAgent(mcp).handle(new CompanyProfile("MCD", "McDonald's"));

        ArgumentCaptor<Map<String, Object>> argsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(mcp).callTool(eq("web_data_yahoo_finance_business"), eq("MCD"), argsCaptor.capture());

        assertThat(argsCaptor.getValue())
                .containsEntry("url", "https://finance.yahoo.com/quote/MCD/profile")
                .containsEntry("companyName", "McDonald's");
    }
}
