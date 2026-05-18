package com.example.finnews.agent;

import com.example.finnews.mcp.FinancialMcpClient;
import com.example.finnews.model.CompanyProfile;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class NewsAgent {
    private final FinancialMcpClient mcp;

    public Map<String, Object> handle(CompanyProfile company) {
        return Map.of("search", mcp.callTool("search_engine", company.ticker(), Map.of(
                "query", company.searchPrefix() + " latest financial news earnings guidance stock",
                "engine", "google")
        ));
    }
}
