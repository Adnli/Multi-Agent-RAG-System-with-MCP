package com.example.finnews.mcp;

import java.util.Map;

public interface FinancialMcpClient {
    Map<String, Object> callTool(String toolName, Map<String, Object> args);
}
