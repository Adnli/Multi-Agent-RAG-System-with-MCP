# Architecture Blueprint

## 1. System Components and Agent Roles

### Frontend
- Next.js UI for submitting analysis requests and displaying responses.

### Backend (Spring Boot)
- `AnalysisController`: API entrypoint (`POST /api/v1/analysis`).
- `FinancialNewsOrchestrator`: central workflow coordinator.
- `CompanyProfileResolver`: ticker-to-company context normalization.
- `RagKnowledgeService`: retrieval and ingestion of historical snippets.
- `TelemetryService` + audit repositories: metrics and audit trail.

### Agents
- **MarketDataAgent**: collects company/market context through MCP finance tool.
- **NewsAgent**: collects current news and source links through MCP search tool.
- **RiskAgent**: gathers risk-oriented signals via MCP batch search patterns.
- **AnalysisAgent**: merges retrieved context + fresh tool data + user question into final answer.

## 2. Technology Stack
- Backend: Spring Boot 3.4 + Spring AI 1.0
- Model provider: OpenAI Chat (`gpt-4.1-mini` currently configured)
- External data access: Bright Data MCP (SSE)
- Data storage: PostgreSQL (RAG + audit)
- Cache: Redis (MCP result caching)
- Frontend: Next.js
- Infra/runtime: Docker Compose
- Tests: JUnit5, Mockito, Spring MVC test

## 3. Data Flow and Integration Points
1. User sends request with `userId`, `role`, `symbol`, and `userQuestion`.
2. Controller validates request and forwards it to orchestrator.
3. Orchestrator resolves company profile and retrieves relevant RAG context.
4. Orchestrator invokes `MarketDataAgent`, `NewsAgent`, and `RiskAgent`.
5. Agents call MCP tools and return structured payloads/snippets.
6. Orchestrator ingests new source snippets into RAG (provenance fields retained).
7. `AnalysisAgent` builds final response with summary/recommendation/confidence/citations/warnings.
8. Response is returned to UI/client and operational events are logged.

## 4. MCP Tools and Rationale
- `web_data_yahoo_finance_business`: stable company profile/business context.
- `search_engine`: recent finance/news discovery.
- `search_engine_batch`: risk-specific multi-query exploration.

Rationale: this mix covers profile + recency + risk dimensions required by the Financial News Analyst use case.

## 5. Non-Functional Baseline
- Input policy + sanitization + prompt-injection checks.
- PII masking (email/SSN/phone patterns).
- Per-user rate limiting.
- Audit trail in PostgreSQL.
- Metrics endpoints via actuator for health and latency counters.

## 6. Known Constraints for Capstone Scope
- Designed for demo/educational operation.
- Not positioned as financial advice engine.
- Video demonstration and final submission links are provided separately.
