package com.example.finnews.agent;

import com.example.finnews.mcp.FinancialMcpClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class RiskAgent {

    private final FinancialMcpClient mcp;

    public Map<String, Object> handle(String ticker) {
        String t = ticker.trim().toUpperCase();

        return mcp.callTool(
                "search_engine_batch", ticker,
                Map.of(
                        "queries", List.of(
                                Map.of(
                                        "query", t + " risk factors SEC filing 10-K 10-Q",
                                        "engine", "google"
                                ),
                                Map.of(
                                        "query", t + " lawsuit regulatory investigation antitrust",
                                        "engine", "google"
                                ),
                                Map.of(
                                        "query", t + " analyst downgrade guidance cut margin pressure",
                                        "engine", "google"
                                ),
                                Map.of(
                                        "query", t + " supply chain risk competition macroeconomic risk",
                                        "engine", "google"
                                )
                        )
                )
        );
    }
}