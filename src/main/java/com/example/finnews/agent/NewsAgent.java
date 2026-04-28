package com.example.finnews.agent;

import com.example.finnews.mcp.FinancialMcpClient;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class NewsAgent {
    private final FinancialMcpClient mcp;

    public NewsAgent(FinancialMcpClient mcp) {
        this.mcp = mcp;
    }

    public Map<String, Object> handle(String ticker) {
        return collect(ticker);
    }

    public Map<String, Object> collect(String ticker) {
        return Map.of(
                "news", mcp.getCompanyNews(ticker),
                "filings", mcp.getSecFilings(ticker),
                "sentiment", mcp.getNewsSentiment(ticker)
        );
    }
}
