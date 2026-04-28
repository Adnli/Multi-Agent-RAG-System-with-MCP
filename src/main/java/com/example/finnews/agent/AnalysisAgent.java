package com.example.finnews.agent;

import com.example.finnews.model.AnalysisResult;
import com.example.finnews.model.KnowledgeDocument;
import com.example.finnews.model.MarketSnapshot;
import com.example.finnews.model.NewsItem;
import com.example.finnews.rag.InMemoryKnowledgeBase;

import java.util.ArrayList;
import java.util.List;

public class AnalysisAgent {
    private final InMemoryKnowledgeBase kb;

    public AnalysisAgent(InMemoryKnowledgeBase kb) {
        this.kb = kb;
    }

    public AnalysisResult analyze(String question, MarketSnapshot market, List<NewsItem> news) {
        List<KnowledgeDocument> context = kb.retrieve(question + " " + market.symbol(), 2);
        List<String> citations = context.stream().map(KnowledgeDocument::sourceUrl).toList();

        String trend = market.dayChangePercent() < -1 ? "short-term bearish pressure" : "neutral-to-bullish momentum";
        String recommendation = market.dayChangePercent() < -2 ? "HOLD / WAIT FOR CONFIRMATION" : "WATCH FOR ENTRY";
        double confidence = Math.max(0.25, 0.9 - Math.abs(market.dayChangePercent()) / 10.0);

        List<String> warnings = new ArrayList<>();
        if (news.isEmpty()) {
            warnings.add("No recent news found; decision quality reduced.");
        }
        boolean potentialHallucination = citations.isEmpty();
        if (potentialHallucination) {
            warnings.add("Low retrieval support; potential hallucination risk.");
        }

        String summary = "Market for " + market.symbol() + " is showing " + trend +
                "; latest price=" + market.price() + "% change=" + market.dayChangePercent() +
                ". News signals considered: " + news.stream().map(NewsItem::headline).limit(2).toList();

        return new AnalysisResult(summary, recommendation, confidence, potentialHallucination, citations, warnings);
    }
}
