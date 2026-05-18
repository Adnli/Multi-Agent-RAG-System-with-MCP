package com.example.finnews.agent;

import com.example.finnews.mcp.FinancialMcpClient;
import com.example.finnews.model.CompanyProfile;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class MarketDataAgent {
    private final FinancialMcpClient mcp;

    public Map<String, Object> handle(CompanyProfile company) {
        return Map.of("companySummary",
                mcp.callTool("web_data_yahoo_finance_business", company.ticker(), Map.of(
                        "url", "https://finance.yahoo.com/quote/" + company.ticker() + "/profile",
                        "companyName", company.companyName())
                )
        );
    }
}
