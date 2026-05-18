package com.example.finnews.agent;

import com.example.finnews.model.KnowledgeChunk;
import com.example.finnews.rag.InMemoryKnowledgeBase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;

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
                          List<KnowledgeChunk> ragDocs,
                          Map<String, Object> risk) {
        if (chatClient == null) {
            return analyzeWithoutLlm(ticker, question);
        }

        String ragContext = ragDocs.stream()
                .map(doc -> "- " + doc.getTitle() + " | " + doc.getContent() + " | source=" + doc.getSourceUrl())
                .reduce("", (a, b) -> a + "\n" + b);

        return Objects.requireNonNull(chatClient.prompt()
                        .system("You are a conservative financial analyst. Always mention uncertainty and risk.")
                        .user(u -> u.text("""
                                        Ticker: {ticker}
                                        User question: {question}
                                        
                                        Market data from MCP tools:
                                        {market}
                                        
                                        News + filings + sentiment from MCP tools:
                                        {news}
                                        
                                        Risks data from MCP tools:
                                        {risks}
                                        
                                        RAG context:
                                        {rag}
                                        
                                        Return clean JSON: summary, recommendation, confidence, citations, warnings
                                        Where summary is a result. Recommendation is your recommendation. Confidence is a rate, example: 0.62. citations is a sources, example: https://example.com/smth. warnings is your warnings, example: ["Educational use only. Not investment advice."]
                                        Answer in the language in which the question was asked.
                                        """)
                                .param("ticker", ticker)
                                .param("question", question)
                                .param("market", marketData.toString())
                                .param("news", newsData.toString())
                                .param("rag", ragContext)
                                .param("risks", risk))
                        .call()
                        .content()).trim().replaceFirst("^```json\\s*", "")
                .replaceFirst("^```\\s*", "")
                .replaceFirst("\\s*```$", "")
                .trim();
    }

    private String analyzeWithoutLlm(String ticker,
                                     String question) {
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
