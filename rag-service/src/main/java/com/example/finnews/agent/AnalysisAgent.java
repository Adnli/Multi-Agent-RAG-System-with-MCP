package com.example.finnews.agent;

import com.example.finnews.model.KnowledgeChunk;
import com.example.finnews.rag.InMemoryKnowledgeBase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class AnalysisAgent {
    private final ChatClient chatClient;
    private final InMemoryKnowledgeBase legacyKnowledgeBase;

    @Autowired
    public AnalysisAgent(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
        this.legacyKnowledgeBase = null;
    }

    public AnalysisAgent(InMemoryKnowledgeBase legacyKnowledgeBase) {
        this.chatClient = null;
        this.legacyKnowledgeBase = legacyKnowledgeBase;
    }

    public String analyze(String ticker,
                          String question,
                          Map<String, Object> marketData,
                          Map<String, Object> newsData,
                          List<KnowledgeChunk> ragDocs) {
        if (chatClient == null) {
            return analyzeWithoutLlm(ticker, question, marketData, newsData);
        }

        String ragContext = ragDocs.stream()
                .map(doc -> "- " + doc.getTitle() + " | " + doc.getContent() + " | source=" + doc.getSourceUrl())
                .reduce("", (a, b) -> a + "\n" + b);

        return chatClient.prompt()
                .system("You are a conservative financial analyst. Always mention uncertainty and risk.")
                .user(u -> u.text("""
                        Ticker: {ticker}
                        User question: {question}

                        Market data from MCP tools:
                        {market}

                        News + filings + sentiment from MCP tools:
                        {news}

                        RAG context:
                        {rag}

                        Return concise JSON with keys: summary, recommendation, confidence, warnings.
                        """)
                        .param("ticker", ticker)
                        .param("question", question)
                        .param("market", marketData.toString())
                        .param("news", newsData.toString())
                        .param("rag", ragContext))
                .call()
                .content();
    }

    private String analyzeWithoutLlm(String ticker,
                                     String question,
                                     Map<String, Object> marketData,
                                     Map<String, Object> newsData) {
        var docs = legacyKnowledgeBase.retrieve(question + " " + ticker, 3);
        String citations = docs.stream()
                .map(doc -> "\"" + doc.sourceUrl() + "\"")
                .reduce((left, right) -> left + "," + right)
                .orElse("");

        return """
                {"summary":"%s analysis used simulated market and news data.","recommendation":"Hold / monitor risk.","confidence":0.75,"citations":[%s],"warnings":["Educational use only. Not investment advice."]}
                """.formatted(ticker, citations).trim();
    }
}
