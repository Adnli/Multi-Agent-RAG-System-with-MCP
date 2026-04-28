"use client";

import { useState } from "react";

export default function Home() {
  const [symbol, setSymbol] = useState("NVDA");
  const [userQuestion, setUserQuestion] = useState("Оцени риски и дай консервативную стратегию");
  const [result, setResult] = useState(null);
  const [error, setError] = useState("");

  const submit = async () => {
    setError("");
    try {
      const res = await fetch(`${process.env.NEXT_PUBLIC_API_BASE}/api/v1/analysis`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ userId: "demo-user", role: "student", symbol, userQuestion })
      });
      if (!res.ok) throw new Error(`HTTP ${res.status}`);
      setResult(await res.json());
    } catch (e) {
      setError(e.message);
    }
  };

  return (
    <main style={{ maxWidth: 900, margin: "40px auto", fontFamily: "Arial" }}>
      <h1>Financial News Analyst</h1>
      <p>Spring Boot + Spring AI MCP + PostgreSQL + Next.js</p>
      <input value={symbol} onChange={e => setSymbol(e.target.value)} placeholder="Ticker" />
      <br /><br />
      <textarea value={userQuestion} onChange={e => setUserQuestion(e.target.value)} rows={4} style={{ width: "100%" }} />
      <br /><br />
      <button onClick={submit}>Analyze</button>

      {error && <p style={{ color: "red" }}>{error}</p>}
      {result && <pre>{JSON.stringify(result, null, 2)}</pre>}
    </main>
  );
}
