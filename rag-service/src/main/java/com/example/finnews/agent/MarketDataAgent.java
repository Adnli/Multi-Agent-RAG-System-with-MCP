package com.example.finnews.agent;

import com.example.finnews.mcp.FinancialMcpClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class MarketDataAgent {
    private final FinancialMcpClient mcp;

    public Map<String, Object> handle(String ticker) {
        return Map.of("companySummary",
                mcp.callTool("web_data_yahoo_finance_business", Map.of("url", "https://finance.yahoo.com/quote/" + ticker + "/profile")
                )
        );
    }
}
