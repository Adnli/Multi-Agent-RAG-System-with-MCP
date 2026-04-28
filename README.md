# Financial News Analyst — Spring Boot + Spring AI + MCP + PostgreSQL + Next.js

Проект переведен на requested стек:
- **Backend:** Spring Boot, Spring AI, Spring AI MCP Client, PostgreSQL
- **Infra:** Docker Compose
- **Frontend:** React / Next.js
- **LLM:** OpenAI (ключ только через env, не в коде)

## Архитектура

```text
FinancialNewsOrchestrator (Spring Boot)
   |
   +--> MarketDataAgent -----> MCP Client -----> financial-mcp-server -----> Alpha Vantage / Finnhub / SEC
   |
   +--> NewsAgent -----------^ 
   |
   +--> AnalysisAgent (Spring AI ChatClient + RAG)
```

Orchestrator **не знает** детали внешних API, а вызывает только MCP tools:
- `get_stock_quote(ticker)`
- `get_daily_prices(ticker, from, to)`
- `get_company_news(ticker)`
- `get_sec_filings(ticker)`
- `get_company_facts(cik)`
- `get_news_sentiment(ticker)`

## Backend API

`POST /api/v1/analysis`

```json
{
  "userId": "u-123",
  "role": "student",
  "symbol": "NVDA",
  "userQuestion": "Оцени риски и дай консервативный план"
}
```

## Security / Safety
- Санитизация input
- Фильтрация adversarial prompt injection
- Маскирование PII
- RBAC
- Rate limit
- Audit trail (PostgreSQL)

## Observability
- Actuator metrics
- Micrometer counters/timers
- success/error rate, latency

## RAG
- `KnowledgeChunk` хранится в PostgreSQL
- Retrieval top-k через `RagService`
- Источники отдаются в `citations`

## Запуск в Docker Compose

1. Создай `.env` в корне:

```bash
OPENAI_API_KEY=your_real_key_here
MCP_SERVER_URL=http://financial-mcp-server:8081
```

2. Подними все сервисы:

```bash
docker compose up --build
```

Сервисы:
- Backend: `http://localhost:8080`
- Frontend: `http://localhost:3000`
- Postgres: `localhost:5432`

## Важно про ключ OpenAI
Ключ должен передаваться только через env (`OPENAI_API_KEY`).
Не коммить ключ в репозиторий и лучше сразу ротировать ключ, если он уже где-то был опубликован.
