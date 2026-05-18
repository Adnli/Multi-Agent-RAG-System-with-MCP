package com.example.finnews.mcp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class SpringAiMcpFinancialClient implements FinancialMcpClient {
    private final SyncMcpToolCallbackProvider callbackProvider;
    private final ObjectMapper objectMapper;
    private final StringRedisTemplate redisTemplate;

    @Value("${app.mcp.cache.ttl-seconds:300}")
    private long cacheTtlSeconds;

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> callTool(String toolName, Map<String, Object> args) {
        String input = toJson(args);
        String cacheKey = cacheKey(toolName);
        Map<String, Object> cachedValue = getCachedValue(cacheKey);
        if (cachedValue != null) {
            return cachedValue;
        }

        Object result = Arrays.stream(callbackProvider.getToolCallbacks())
                .filter(callback -> {
                    String actualName = callback.getToolDefinition().name();
                    return actualName.equals(toolName) || actualName.endsWith("_" + toolName);
                })
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "MCP tool not found: " + toolName + ". Available tools: " +
                                Arrays.stream(callbackProvider.getToolCallbacks())
                                        .map(callback -> callback.getToolDefinition().name())
                                        .toList()
                ))
                .call(input);
        Map<String, Object> normalizedResult;

        if (result instanceof Map<?, ?> rawMap) {
            normalizedResult = (Map<String, Object>) rawMap;
        } else if (result instanceof String text) {
            normalizedResult = fromJson(text);
        } else {
            normalizedResult = new HashMap<>(Map.of("result", result));
        }
        if(toolName.equals("web_data_yahoo_finance_business")){
            cacheValue(cacheKey, normalizedResult);
        }
        return normalizedResult;
    }

    private String toJson(Map<String, Object> args) {
        try {
            return objectMapper.writeValueAsString(args);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to serialize MCP tool arguments.", e);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> fromJson(String value) {
        try {
            Object parsed = objectMapper.readValue(value, Object.class);
            if (parsed instanceof Map<?, ?> rawMap) {
                return (Map<String, Object>) rawMap;
            }
            return new HashMap<>(Map.of("result", parsed));
        } catch (JsonProcessingException e) {
            return new HashMap<>(Map.of("result", value));
        }
    }

    private Map<String, Object> getCachedValue(String cacheKey) {
        String cachedJson = redisTemplate.opsForValue().get(cacheKey);
        if (cachedJson == null || cachedJson.isBlank()) {
            return null;
        }
        return fromJson(cachedJson);
    }

    private void cacheValue(String cacheKey, Map<String, Object> value) {
        redisTemplate.opsForValue().set(cacheKey, toJson(value), Duration.ofSeconds(cacheTtlSeconds));
    }

    private String cacheKey(String toolName) {
        return "mcp:tool:%s".formatted(toolName);
    }
}