# Multi-Agent RAG Financial News Analyst
A Spring Boot financial analysis service built with multiple AI agents, Spring AI, the external Bright Data MCP server, PostgreSQL-backed RAG, Redis caching, and a Next.js frontend.

## Stack

- Backend: Spring Boot 3.4, Spring AI 1.0, Spring AI MCP Client
- LLM: OpenAI Chat model `gpt-4.1-mini`
- External data: Bright Data MCP over SSE
- Storage: PostgreSQL for audit/RAG, Redis for MCP tool cache
- Frontend: Next.js
- Infrastructure: Docker Compose
- Tests: JUnit 5, Spring MVC test, Mockito, AssertJ

## Architecture

```text
Frontend
   |
   v
AnalysisController
   |
   v
FinancialNewsOrchestrator
   |
   +--> CompanyProfileResolver
   |       V    -> Visa
   |       AAPL -> Apple
   |       AMZN -> Amazon
   |       MCD  -> McDonald's
   |
   +--> RagKnowledgeService
   |       retrieve existing trusted context from PostgreSQL
   |
   +--> MarketDataAgent -> MCP: web_data_yahoo_finance_business
   +--> NewsAgent       -> MCP: search_engine
   +--> RiskAgent       -> MCP: search_engine_batch
   |
   +--> RagKnowledgeService
   |       ingest fresh source snippets for future requests
   |
   +--> AnalysisAgent -> OpenAI ChatClient
```

Core RAG rule: fresh MCP payload is passed directly to the LLM as tool data, while only source snippets are stored in RAG for future requests. Final LLM answers and recommendations are not written back into the knowledge base.

## Bright Data MCP

The application uses the Bright Data SSE endpoint:

```yaml
spring:
  ai:
    mcp:
      client:
        sse:
          connections:
            financial:
              url: ${MCP_SERVER_URL}
              sse-endpoint: /sse?token=${MCP_SERVER_TOKEN}&groups=advanced_scraping,finance
```

Example values:

```env
MCP_SERVER_URL=https://mcp.brightdata.com
MCP_SERVER_TOKEN=your_bright_data_token
```

Used MCP tools:

- `web_data_yahoo_finance_business` - company profile from Yahoo Finance
- `search_engine` - news and recent financial sources
- `search_engine_batch` - risk, SEC-related, lawsuit, downgrade, and supply-chain searches

## RAG

`RagKnowledgeService` stores `KnowledgeChunk` entities in PostgreSQL.

Stored provenance fields:

- `ticker` - normalized ticker symbol
- `companyName` - human-readable company name
- `sourceUrl` - real external source URL
- `sourceType` - currently `mcp_tool_result`
- `sourceName` - MCP server name, for example `brightdata-mcp`
- `mcpToolName` - MCP tool name, for example `search_engine`
- `dataProvider` - upstream engine/provider, for example `google`
- `queryText` - query used to discover the source
- `contentKey` - stable deduplication key based on `ticker + normalized sourceUrl`
- `createdAt` - save/update timestamp

Retrieval behavior:

- Searches top chunks using `ticker + companyName + userQuestion`
- If there is no lexical match but chunks exist for the ticker, returns the latest ticker chunks as a fallback
- Runs before fresh MCP calls so the current request is not mixed into the RAG context of the same request

## API

Endpoint:

```http
POST /api/v1/analysis
Content-Type: application/json
```

Request:

```json
{
  "userId": "u-123",
  "role": "analyst",
  "symbol": "AAPL",
  "userQuestion": "Analyze Apple using recent news, risk data, and RAG context"
}
```

Supported roles:

- `student`
- `analyst`
- `admin`

Ticker associations:

```text
V    = Visa
AAPL = Apple
AMZN = Amazon
MCD  = McDonald's
```

Response:

```json
{
  "summary": "string",
  "recommendation": "string",
  "confidence": 0.72,
  "citations": ["https://source.example"],
  "toolCalls": [
    "web_data_yahoo_finance_business",
    "search_engine",
    "search_engine_batch"
  ],
  "warnings": ["Educational use only. Not investment advice."]
}
```

## Safety

The backend performs:

- Role validation
- Per-user rate limiting
- Input sanitization
- PII masking for email, SSN, and phone numbers
- Prompt-injection blocklist checks
- Audit trail in PostgreSQL
- Micrometer success/error counters and latency timer

## Environment

Create a `.env` file in the project root:

```env
OPENAI_API_KEY=your_openai_key

MCP_SERVER_URL=https://mcp.brightdata.com
MCP_SERVER_TOKEN=your_bright_data_token

SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/finnews
SPRING_DATASOURCE_USERNAME=finnews_user
SPRING_DATASOURCE_PASSWORD=change_me

REDIS_HOST=redis
NEXT_PUBLIC_API_BASE=http://backend:8080
```

For local IntelliJ runs without Docker:

```env
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/finnews
REDIS_HOST=localhost
NEXT_PUBLIC_API_BASE=http://localhost:8080
```

Do not commit secrets. If a token or OpenAI key has already appeared in chat, logs, or the repository, rotate it.

## Run

```bash
docker compose up --build
```

Services:

- Backend: `http://localhost:8080`
- Frontend: `http://localhost:3000`
- PostgreSQL: `localhost:5432`
- Redis: `localhost:6379`
- RedisInsight: `http://localhost:5540`

Health and metrics:

```text
http://localhost:8080/actuator/health
http://localhost:8080/actuator/metrics
```

## Tests

Locally, if Maven is installed:

```bash
cd rag-service
mvn test
```

Using Docker Maven:

```bash
cd rag-service
docker run --rm -v "${PWD}:/app" -w /app maven:3.9.9-eclipse-temurin-17 mvn test
```

Covered areas:

- HTTP validation in `AnalysisController`
- Input safety and PII masking
- Rate limiting
- Company profile resolution
- Query construction in `MarketDataAgent`, `NewsAgent`, and `RiskAgent`
- Orchestrator contract: RAG retrieval order, MCP calls, ingest, analysis
- RAG ingestion, dedup/update, provenance, parsing of multiple MCP payload formats, retrieval fallback

## Production Notes

- RAG must not store final LLM recommendations as knowledge.
- Redis cache is used for expensive MCP tools. TTL is configured via `app.mcp.cache.ttl-seconds`.
- `spring.jpa.hibernate.ddl-auto=update` is convenient for development. For production, prefer Flyway or Liquibase migrations.
- MCP payload logs can contain external data. In production, reduce payload logging or mask sensitive fields.


## Capstone Deliverables

The repository includes the core capstone documents:

- Executive Summary: `docs/Executive_Summary.md`
- Architecture Blueprint: `docs/Architecture_Blueprint.md`
- Self-Review: `docs/Self_Review.md`
- Video Demo section and checklist: `docs/Video_Demo.md`
- Submission file: `Capstone_project_Adil_Bazarbay.txt`

Before final submission, replace the `TODO` video URL placeholders with a public link.
