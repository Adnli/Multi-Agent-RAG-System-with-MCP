# Executive Summary

## Problem Statement
Financial analysts and learners need faster ways to combine fragmented market context (company profile, current news, and risk signals) into one consistent, source-aware view. Manual analysis is slow, difficult to reproduce, and error-prone when information changes quickly.

## Project Objective
This project delivers a **multi-agent Financial News Analyst** that automates this workflow using:
- external data collection via MCP tools,
- retrieval of historical context from a local RAG store,
- final synthesis by an LLM with citations and safety warnings.

The system is designed for educational and analyst-assist scenarios, not autonomous trading.

## Solution Overview
The application provides a backend API and a frontend UI. A request (user role + ticker + question) is processed by an orchestrator that coordinates specialized agents:
- **MarketDataAgent** for company/market context,
- **NewsAgent** for current financial news,
- **RiskAgent** for risk-oriented signals,
- **AnalysisAgent** for final narrative, recommendation, confidence, and citations.

Historical snippets are persisted in PostgreSQL as RAG knowledge chunks, while fresh tool outputs are used in the current analysis. This preserves traceability and avoids storing final LLM recommendations as knowledge facts.

## Key Technical Decisions
1. **Multi-agent decomposition** instead of one monolithic prompt to improve clarity of responsibilities and easier testing.
2. **MCP integration** for external data access with explicit tool-call provenance.
3. **PostgreSQL-backed RAG** with dedup/provenance fields for repeatable retrieval.
4. **Safety layer** including input checks, role validation, PII masking, and rate limiting.
5. **Observability baseline** via audit events and metrics endpoints.

## Results and Current Status
The repository already includes:
- functional multi-agent orchestration,
- MCP-based data retrieval,
- RAG ingestion/retrieval logic,
- API/controller/service tests,
- Docker-based local run setup.

For capstone submission, required reporting artifacts are now included in `docs/` together with a submission file template and checklist references.

## Business Value
- Reduces analyst time-to-insight by consolidating multiple sources into one response.
- Improves consistency through reusable orchestration and retrieval logic.
- Supports transparent outputs with citations and explicit warnings.

## Next Steps
- Add final demo video link and run-through evidence.
- Expand quality metrics for retrieval relevance and hallucination checks.
- Harden production controls (authn/authz, stronger compliance workflows) if moving beyond educational/demo scope.
