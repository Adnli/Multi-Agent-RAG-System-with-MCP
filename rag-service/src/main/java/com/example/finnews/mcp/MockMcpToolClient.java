package com.example.finnews.mcp;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MockMcpToolClient implements McpToolClient, FinancialMcpClient {
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

    @Override
    public Map<String, Object> getStockQuote(String ticker) {
        return callTool("market_data.get_quote", Map.of("symbol", ticker));
    }

    @Override
    public Map<String, Object> getDailyPrices(String ticker, String from, String to) {
        return Map.of(
                "symbol", ticker,
                "from", from,
                "to", to,
                "prices", List.of(208.10, 211.40, 212.45),
                "provider", "mcp-market-sim"
        );
    }

    @Override
    public Map<String, Object> getCompanyNews(String ticker) {
        return callTool("news.search", Map.of("symbol", ticker));
    }

    @Override
    public Map<String, Object> getSecFilings(String ticker) {
        return Map.of(
                "symbol", ticker,
                "filings", List.of("10-Q", "8-K"),
                "provider", "mcp-sec-sim"
        );
    }

    @Override
    public Map<String, Object> getCompanyFacts(String cik) {
        return Map.of(
                "cik", cik,
                "facts", Map.of("revenueGrowth", "simulated"),
                "provider", "mcp-facts-sim"
        );
    }

    @Override
    public Map<String, Object> getNewsSentiment(String ticker) {
        return Map.of(
                "symbol", ticker,
                "sentiment", "neutral",
                "score", 0.1,
                "provider", "mcp-sentiment-sim"
        );
    }
}
