package com.example.finnews.mcpserver;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class FinancialTools {

    @Tool(name = "get_stock_quote", description = "Return a simulated real-time stock quote for a ticker symbol.")
    public Map<String, Object> getStockQuote(
            @ToolParam(description = "Ticker symbol, for example AAPL, MSFT, NVDA.") String ticker) {
        String symbol = normalizeTicker(ticker);
        double basePrice = basePrice(symbol);
        double changePercent = round(((Math.abs(symbol.hashCode()) % 700) - 350) / 100.0);

        return Map.of(
                "symbol", symbol,
                "price", basePrice,
                "dayChangePercent", changePercent,
                "currency", "USD",
                "timestamp", OffsetDateTime.now().toString(),
                "provider", "local-spring-ai-mcp"
        );
    }

    @Tool(name = "get_daily_prices", description = "Return simulated daily closing prices for a ticker and date range.")
    public Map<String, Object> getDailyPrices(
            @ToolParam(description = "Ticker symbol.") String ticker,
            @ToolParam(description = "Start date in ISO-8601 format, yyyy-MM-dd.") String from,
            @ToolParam(description = "End date in ISO-8601 format, yyyy-MM-dd.") String to) {
        String symbol = normalizeTicker(ticker);
        LocalDate start = parseDate(from, LocalDate.now().minusDays(7));
        LocalDate end = parseDate(to, LocalDate.now());
        double seed = basePrice(symbol);

        List<Map<String, Object>> prices = start.datesUntil(end.plusDays(1))
                .limit(31)
                .map(date -> Map.<String, Object>of(
                        "date", date.toString(),
                        "close", round(seed + ((date.toEpochDay() % 9) - 4) * 1.35)
                ))
                .toList();

        return Map.of(
                "symbol", symbol,
                "from", start.toString(),
                "to", end.toString(),
                "prices", prices,
                "provider", "local-spring-ai-mcp"
        );
    }

    @Tool(name = "get_company_news", description = "Return recent simulated company news for a ticker symbol.")
    public Map<String, Object> getCompanyNews(
            @ToolParam(description = "Ticker symbol.") String ticker) {
        String symbol = normalizeTicker(ticker);
        return Map.of(
                "symbol", symbol,
                "items", List.of(
                        Map.of(
                                "headline", symbol + " reports resilient demand despite macro uncertainty",
                                "source", "Local Market Wire",
                                "content", "Management commentary points to cautious but stable demand trends.",
                                "timestamp", OffsetDateTime.now().minusHours(3).toString()
                        ),
                        Map.of(
                                "headline", "Analysts debate valuation risk for " + symbol,
                                "source", "Local Analyst Desk",
                                "content", "Recent price movement raises questions about margin of safety.",
                                "timestamp", OffsetDateTime.now().minusHours(7).toString()
                        )
                ),
                "provider", "local-spring-ai-mcp"
        );
    }

    @Tool(name = "get_sec_filings", description = "Return recent simulated SEC filings for a ticker symbol.")
    public Map<String, Object> getSecFilings(
            @ToolParam(description = "Ticker symbol.") String ticker) {
        String symbol = normalizeTicker(ticker);
        return Map.of(
                "symbol", symbol,
                "filings", List.of(
                        Map.of("form", "10-Q", "filedAt", LocalDate.now().minusDays(21).toString(), "summary", "Quarterly update with standard risk disclosures."),
                        Map.of("form", "8-K", "filedAt", LocalDate.now().minusDays(8).toString(), "summary", "Corporate event disclosure.")
                ),
                "provider", "local-spring-ai-mcp"
        );
    }

    @Tool(name = "get_company_facts", description = "Return simulated company facts for a CIK identifier.")
    public Map<String, Object> getCompanyFacts(
            @ToolParam(description = "SEC CIK identifier.") String cik) {
        String normalizedCik = cik == null || cik.isBlank() ? "unknown" : cik.trim();
        return Map.of(
                "cik", normalizedCik,
                "facts", Map.of(
                        "revenueGrowth", "mid-single-digit simulated growth",
                        "grossMarginTrend", "stable",
                        "debtProfile", "manageable"
                ),
                "provider", "local-spring-ai-mcp"
        );
    }

    @Tool(name = "get_news_sentiment", description = "Return simulated news sentiment for a ticker symbol.")
    public Map<String, Object> getNewsSentiment(
            @ToolParam(description = "Ticker symbol.") String ticker) {
        String symbol = normalizeTicker(ticker);
        double score = round(((Math.abs(symbol.hashCode()) % 200) - 100) / 100.0);
        String sentiment = score > 0.25 ? "positive" : score < -0.25 ? "negative" : "neutral";

        return Map.of(
                "symbol", symbol,
                "sentiment", sentiment,
                "score", score,
                "provider", "local-spring-ai-mcp"
        );
    }

    private String normalizeTicker(String ticker) {
        if (ticker == null || ticker.isBlank()) {
            return "SPY";
        }
        return ticker.trim().toUpperCase(Locale.ROOT);
    }

    private LocalDate parseDate(String value, LocalDate fallback) {
        try {
            return value == null || value.isBlank() ? fallback : LocalDate.parse(value);
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    private double basePrice(String symbol) {
        return round(75.0 + Math.abs(symbol.hashCode() % 30000) / 100.0);
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
