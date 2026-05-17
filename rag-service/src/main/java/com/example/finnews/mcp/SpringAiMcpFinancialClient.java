package com.example.finnews.mcp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class SpringAiMcpFinancialClient implements FinancialMcpClient {
    private final SyncMcpToolCallbackProvider callbackProvider;
    private final ObjectMapper objectMapper;

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> callTool(String toolName, Map<String, Object> args) {
        String input = toJson(args);
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

        if (result instanceof Map<?, ?> rawMap) {
            return (Map<String, Object>) rawMap;
        }
        if (result instanceof String text) {
            return fromJson(text);
        }
        return new HashMap<>(Map.of("result", result));
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
}
//20:06:43
//20:10:00