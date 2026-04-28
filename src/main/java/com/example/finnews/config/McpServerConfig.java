package com.example.finnews.config;

import org.springframework.context.annotation.Configuration;

@Configuration
public class McpServerConfig {
    // Spring AI MCP autoconfiguration is enabled via application.yml.
    // Configure MCP endpoint in properties:
    // spring.ai.mcp.client.sse.connections.financial.url=http://financial-mcp-server:8081
}
