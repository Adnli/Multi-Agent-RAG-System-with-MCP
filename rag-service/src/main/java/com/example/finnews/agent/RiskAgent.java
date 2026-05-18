package com.example.finnews.agent;

import com.example.finnews.mcp.FinancialMcpClient;
import com.example.finnews.model.CompanyProfile;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class RiskAgent {

    private final FinancialMcpClient mcp;

    public Map<String, Object> handle(CompanyProfile company) {
        String queryPrefix = company.searchPrefix();

        return mcp.callTool(
                "search_engine_batch", company.ticker(),
                Map.of(
                        "queries", List.of(
                                Map.of(
                                        "query", queryPrefix + " risk factors SEC filing 10-K 10-Q",
                                        "engine", "google"
                                ),
                                Map.of(
                                        "query", queryPrefix + " lawsuit regulatory investigation antitrust",
                                        "engine", "google"
                                ),
                                Map.of(
                                        "query", queryPrefix + " analyst downgrade guidance cut margin pressure",
                                        "engine", "google"
                                ),
                                Map.of(
                                        "query", queryPrefix + " supply chain risk competition macroeconomic risk",
                                        "engine", "google"
                                )
                        )
                )
        );
    }
}
