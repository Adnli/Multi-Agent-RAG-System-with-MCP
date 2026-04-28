package com.example.finnews.mcp;

import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class SpringAiMcpFinancialClient implements FinancialMcpClient {
    private final SyncMcpToolCallbackProvider callbackProvider;

    public SpringAiMcpFinancialClient(SyncMcpToolCallbackProvider callbackProvider) {
        this.callbackProvider = callbackProvider;
    }

    @Override
    public Map<String, Object> getStockQuote(String ticker) {
        return invoke("get_stock_quote", Map.of("ticker", ticker));
    }

    @Override
    public Map<String, Object> getDailyPrices(String ticker, String from, String to) {
        return invoke("get_daily_prices", Map.of("ticker", ticker, "from", from, "to", to));
    }

    @Override
    public Map<String, Object> getCompanyNews(String ticker) {
        return invoke("get_company_news", Map.of("ticker", ticker));
    }

    @Override
    public Map<String, Object> getSecFilings(String ticker) {
        return invoke("get_sec_filings", Map.of("ticker", ticker));
    }

    @Override
    public Map<String, Object> getCompanyFacts(String cik) {
        return invoke("get_company_facts", Map.of("cik", cik));
    }

    @Override
    public Map<String, Object> getNewsSentiment(String ticker) {
        return invoke("get_news_sentiment", Map.of("ticker", ticker));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> invoke(String toolName, Map<String, Object> args) {
        Object result = callbackProvider.getToolCallbacks().stream()
                .filter(callback -> callback.getToolDefinition().name().equals(toolName))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("MCP tool not found: " + toolName))
                .call(args);

        if (result instanceof Map<?, ?> rawMap) {
            return (Map<String, Object>) rawMap;
        }
        return new HashMap<>(Map.of("result", result));
    }
}
