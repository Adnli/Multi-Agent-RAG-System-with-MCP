"use client";

import { useMemo, useState } from "react";

const starterPrompts = [
  "Оцени риски и дай консервативную стратегию",
  "Что сейчас влияет на цену акции?",
  "Сделай краткий анализ новостей и SEC filings"
];

const tickerOptions = ["V", "AAPL", "AMZN", "MCD", "GLOBAL"];

function AssistantMessage({ content }) {
  if (!content) {
    return null;
  }

  return (
    <div className="analysis-card">
      {content.summary && (
        <section>
          <span className="section-label">Summary</span>
          <p>{content.summary}</p>
        </section>
      )}

      {content.recommendation && (
        <section>
          <span className="section-label">Recommendation</span>
          <p>{content.recommendation}</p>
        </section>
      )}

      <div className="metric-row">
        <div>
          <span className="section-label">Confidence</span>
          <strong>{Math.round((content.confidence || 0) * 100)}%</strong>
        </div>
        <div>
          <span className="section-label">Tool calls</span>
          <strong>{content.toolCalls?.length || 0}</strong>
        </div>
      </div>

      {!!content.warnings?.length && (
        <section>
          <span className="section-label">Warnings</span>
          <ul>
            {content.warnings.map((warning) => (
              <li key={warning}>{warning}</li>
            ))}
          </ul>
        </section>
      )}

      {!!content.citations?.length && (
        <section>
          <span className="section-label">Citations</span>
          <div className="citation-list">
            {content.citations.map((citation) => (
              <span key={citation}>{citation}</span>
            ))}
          </div>
        </section>
      )}
    </div>
  );
}

export default function Home() {
  const [symbol, setSymbol] = useState("V");
  const [userQuestion, setUserQuestion] = useState("");
  const [messages, setMessages] = useState([
    {
      id: "welcome",
      role: "assistant",
      content: {
        summary: "Привет. Я могу проанализировать финансовые новости, рыночные данные и доступные источники по тикеру.",
        recommendation: "Выбери тикер, задай вопрос и отправь запрос на анализ.",
        confidence: 1,
        citations: [],
        toolCalls: [],
        warnings: []
      }
    }
  ]);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState("");

  const trimmedQuestion = userQuestion.trim();
  const canSubmit = useMemo(
    () => symbol.trim().length > 0 && trimmedQuestion.length > 0 && !isLoading,
    [isLoading, symbol, trimmedQuestion]
  );

  const submit = async (question = trimmedQuestion) => {
    if (!question || isLoading) {
      return;
    }

    const normalizedSymbol = symbol.trim().toUpperCase();
    const userMessage = {
      id: `${Date.now()}-user`,
      role: "user",
      symbol: normalizedSymbol,
      content: question
    };

    setMessages((current) => [...current, userMessage]);
    setUserQuestion("");
    setError("");
    setIsLoading(true);

    try {
      const res = await fetch(`${process.env.NEXT_PUBLIC_API_BASE}/api/v1/analysis`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          userId: "demo-user",
          role: "student",
          symbol: normalizedSymbol,
          userQuestion: question
        })
      });

      if (!res.ok) {
        throw new Error(`Backend returned HTTP ${res.status}`);
      }

      const data = await res.json();
      setMessages((current) => [
        ...current,
        {
          id: `${Date.now()}-assistant`,
          role: "assistant",
          content: data
        }
      ]);
    } catch (e) {
      setError(e.message);
      setMessages((current) => [
        ...current,
        {
          id: `${Date.now()}-error`,
          role: "assistant",
          content: {
            summary: "Не удалось получить ответ от backend.",
            recommendation: e.message,
            confidence: 0,
            citations: [],
            toolCalls: [],
            warnings: ["Проверь, что backend запущен и NEXT_PUBLIC_API_BASE настроен правильно."]
          }
        }
      ]);
    } finally {
      setIsLoading(false);
    }
  };

  const handleKeyDown = (event) => {
    if (event.key === "Enter" && !event.shiftKey) {
      event.preventDefault();
      if (canSubmit) {
        submit();
      }
    }
  };

  return (
    <main className="chat-shell">
      <aside className="sidebar">
        <div className="brand">
          <div className="brand-mark">F</div>
          <div>
            <h1>FinNews Analyst</h1>
            <p>Market RAG assistant</p>
          </div>
        </div>

        <button className="new-chat" onClick={() => setMessages(messages.slice(0, 1))}>
          New analysis
        </button>

        <div className="sidebar-section">
          <span>Examples</span>
          {starterPrompts.map((prompt) => (
            <button
              className="prompt-button"
              key={prompt}
              onClick={() => setUserQuestion(prompt)}
            >
              {prompt}
            </button>
          ))}
        </div>
      </aside>

      <section className="chat-panel">
        <header className="topbar">
          <div>
            <span className="eyebrow">Financial intelligence</span>
            <h2>Analysis Chat</h2>
          </div>
          <label className="ticker-control">
            <span>Ticker</span>
            <select
              value={symbol}
              onChange={(e) => setSymbol(e.target.value)}
            >
              {tickerOptions.map((ticker) => (
                <option key={ticker} value={ticker}>
                  {ticker}
                </option>
              ))}
            </select>
          </label>
        </header>

        <div className="message-list">
          {messages.map((message) => (
            <article className={`message ${message.role}`} key={message.id}>
              <div className="avatar">{message.role === "user" ? "U" : "AI"}</div>
              <div className="bubble">
                {message.role === "user" ? (
                  <>
                    <span className="message-symbol">{message.symbol}</span>
                    <p>{message.content}</p>
                  </>
                ) : (
                  <AssistantMessage content={message.content} />
                )}
              </div>
            </article>
          ))}

          {isLoading && (
            <article className="message assistant">
              <div className="avatar">AI</div>
              <div className="bubble loading-bubble">
                <span />
                <span />
                <span />
              </div>
            </article>
          )}
        </div>

        <footer className="composer-wrap">
          {error && <div className="error-banner">{error}</div>}
          <div className="composer">
            <textarea
              value={userQuestion}
              onChange={(e) => setUserQuestion(e.target.value)}
              onKeyDown={handleKeyDown}
              placeholder="Ask about risks, news, filings, sentiment, or a trading thesis..."
              rows={1}
            />
            <button disabled={!canSubmit} onClick={() => submit()} aria-label="Send message">
              ↑
            </button>
          </div>
          <p className="disclaimer">AI analysis can be wrong. Verify important financial decisions.</p>
        </footer>
      </section>
    </main>
  );
}
