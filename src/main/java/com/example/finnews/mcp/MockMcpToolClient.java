package com.example.finnews.mcp;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MockMcpToolClient implements McpToolClient {
    @Override
    public Map<String, Object> callTool(String toolName, Map<String, Object> args) {
        if ("market_data.get_quote".equals(toolName)) {
            String symbol = args.getOrDefault("symbol", "SPY").toString();
            return Map.of(
                    "symbol", symbol,
                    "price", 212.45,
                    "dayChangePercent", -1.8,
                    "timestamp", Instant.now().toString(),
                    "provider", "mcp-market-sim"
            );
        }
        if ("news.search".equals(toolName)) {
            List<Map<String, Object>> items = List.of(
                    new HashMap<>(Map.of("headline", "Fed signals cautious approach", "source", "Reuters", "content", "Rates may stay elevated.", "timestamp", Instant.now().toString())),
                    new HashMap<>(Map.of("headline", "AI chip demand remains strong", "source", "Bloomberg", "content", "Semiconductor outlook resilient.", "timestamp", Instant.now().toString()))
            );
            return Map.of("items", items, "provider", "mcp-news-sim");
        }
        throw new IllegalArgumentException("Unsupported MCP tool: " + toolName);
    }
}
