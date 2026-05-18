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
        return Map.of("search", mcp.callTool("search_engine", Map.of(
                "query", ticker + " latest financial news earnings guidance stock",
                "engine", "google")
        ));
    }
}
