package com.example.finnews.mcpserver;

import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class FinancialMcpServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(FinancialMcpServerApplication.class, args);
    }

    @Bean
    ToolCallbackProvider financialToolCallbacks(FinancialTools financialTools) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(financialTools)
                .build();
    }
}
