package com.example.finnews.mcp;

import java.util.Map;

public interface FinancialMcpClient {
    Map<String, Object> getStockQuote(String ticker);
    Map<String, Object> getDailyPrices(String ticker, String from, String to);
    Map<String, Object> getCompanyNews(String ticker);
    Map<String, Object> getSecFilings(String ticker);
    Map<String, Object> getCompanyFacts(String cik);
    Map<String, Object> getNewsSentiment(String ticker);
}
