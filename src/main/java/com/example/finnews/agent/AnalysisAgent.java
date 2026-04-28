package com.example.finnews.agent;

import com.example.finnews.model.KnowledgeChunk;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class AnalysisAgent {
    private final ChatClient chatClient;

    public AnalysisAgent(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    public String analyze(String ticker,
                          String question,
                          Map<String, Object> marketData,
                          Map<String, Object> newsData,
                          List<KnowledgeChunk> ragDocs) {

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
}
