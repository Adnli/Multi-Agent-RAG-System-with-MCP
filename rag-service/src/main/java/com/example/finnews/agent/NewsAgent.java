package com.example.finnews.agent;

import com.example.finnews.mcp.FinancialMcpClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class NewsAgent {
    private final FinancialMcpClient mcp;

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
