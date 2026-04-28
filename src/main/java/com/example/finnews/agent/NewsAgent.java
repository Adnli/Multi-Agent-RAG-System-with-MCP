package com.example.finnews.agent;

import com.example.finnews.mcp.McpToolClient;
import com.example.finnews.model.NewsItem;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class NewsAgent implements Agent<String, List<NewsItem>> {
    private final McpToolClient mcp;

    public NewsAgent(McpToolClient mcp) {
        this.mcp = mcp;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<NewsItem> handle(String symbol) {
        Map<String, Object> result = mcp.callTool("news.search", Map.of("query", symbol));
        List<Map<String, Object>> items = (List<Map<String, Object>>) result.get("items");
        List<NewsItem> parsed = new ArrayList<>();
        for (Map<String, Object> item : items) {
            parsed.add(new NewsItem(
                    item.get("headline").toString(),
                    item.get("source").toString(),
                    item.get("content").toString(),
                    Instant.parse(item.get("timestamp").toString())
            ));
        }
        return parsed;
    }
}
