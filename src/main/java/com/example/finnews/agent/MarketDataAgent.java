package com.example.finnews.agent;

import com.example.finnews.mcp.FinancialMcpClient;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Map;

@Component
public class MarketDataAgent {
    private final FinancialMcpClient mcp;

    public MarketDataAgent(FinancialMcpClient mcp) {
        this.mcp = mcp;
    }

    public Map<String, Object> handle(String ticker) {
        return collect(ticker);
    }

    public Map<String, Object> collect(String ticker) {
        LocalDate to = LocalDate.now();
        LocalDate from = to.minusDays(30);
        return Map.of(
                "quote", mcp.getStockQuote(ticker),
                "dailyPrices", mcp.getDailyPrices(ticker, from.toString(), to.toString())
        );
    }
}
