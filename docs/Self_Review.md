# Self-Review

## What Works Well
1. **Clear multi-agent separation**
    - Each agent has one responsibility (market/news/risk/analysis), which simplifies maintenance and testing.
2. **Practical RAG design**
    - Historical snippets are stored with provenance; final LLM recommendations are not re-ingested as facts.
3. **Production-aware baseline**
    - Rate limits, input policy, PII masking, and audit logging are included early.
4. **Test coverage foundation**
    - Unit/service/controller/orchestrator tests exist for core user flow and safety checks.

## Key Trade-offs
1. **Speed vs completeness**
    - Scope prioritized a working end-to-end system over a full enterprise-grade compliance layer.
2. **External data freshness vs reliability**
    - MCP improves freshness but introduces dependency on external tool availability/quality.
3. **Simple role model vs full security**
    - Request role validation is present, but full authentication/authorization stack is intentionally deferred.

## Limitations
- No full authn/authz implementation yet.
- Advanced RAG quality metrics (precision/recall dashboards) are not fully automated.
- Hallucination/bias evaluation is not yet a formal offline evaluation pipeline.

## Why These Decisions Were Reasonable
For capstone goals, the chosen architecture maximizes demonstrable value quickly:
- a functioning multi-agent workflow,
- explicit data provenance,
- testable safety and orchestration behavior,
- deployable local setup via Docker Compose.

## Improvement Roadmap
1. Add authenticated access control and scoped permissions.
2. Add automated quality evaluation jobs for retrieval relevance and factuality.
3. Add richer tracing for token/cost analytics and feedback loop from users.
4. Add load/concurrency testing and resource usage reporting.
