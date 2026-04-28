package com.example.finnews.agent;

import com.example.finnews.mcp.McpToolClient;
import com.example.finnews.model.MarketSnapshot;

import java.time.Instant;
import java.util.Map;

public class MarketDataAgent implements Agent<String, MarketSnapshot> {
    private final McpToolClient mcp;

    public MarketDataAgent(McpToolClient mcp) {
        this.mcp = mcp;
    }

    @Override
    public MarketSnapshot handle(String symbol) {
        Map<String, Object> result = mcp.callTool("market_data.get_quote", Map.of("symbol", symbol));
        return new MarketSnapshot(
                result.get("symbol").toString(),
                Double.parseDouble(result.get("price").toString()),
                Double.parseDouble(result.get("dayChangePercent").toString()),
                Instant.parse(result.get("timestamp").toString())
        );
    }
}
