# Financial News Analyst (Multi-Agent + RAG + MCP) — Java

Проект для курса **Generative AI for Software Developer**.

## Что решает система
Система помогает быстро оценивать рыночную ситуацию по тикеру, объединяя:
1. Текущие рыночные данные.
2. Последние финансовые новости.
3. Исторические закономерности из базы знаний (RAG).

Результат: краткий рыночный анализ + осторожная инвестиционная рекомендация с источниками.

## Архитектура
### Агенты
- **MarketDataAgent**: получает котировки через MCP-инструмент `market_data.get_quote`.
- **NewsAgent**: получает новости через MCP-инструмент `news.search`.
- **AnalysisAgent**: объединяет новости, рынок и RAG-контекст, формирует рекомендацию и флаги риска галлюцинаций.

### Оркестрация
`FinancialNewsOrchestrator` координирует последовательность работы агентов и обеспечивает:
- авторизацию,
- rate limiting,
- санитизацию и фильтрацию,
- аудит-лог,
- telemetry/metrics.

### RAG pipeline
- `InMemoryKnowledgeBase` хранит предметные документы (ставки, earnings, риск-менеджмент).
- Retrieval: семантическое приближение через overlap-score токенов.
- Generation: `AnalysisAgent` использует top-k документы и добавляет citations.

### MCP integration
`McpToolClient` задаёт MCP-контракт для внешних инструментов.
В демо используется `MockMcpToolClient` (эмуляция MCP tools), которую можно заменить на реальный MCP transport.

## Наблюдаемость
- LLM/Agent tracing: `TelemetryCollector` + `TelemetryEvent`.
- Производительность: average latency, success rate.
- Ошибки: failure count.
- Ресурсы: memory + CPU load snapshot.
- Аудит: `AuditLogger`.

## Безопасность
- `InputSanitizer` — очистка входных данных.
- `ContentFilter` — блокировка вредных prompt-инъекций.
- `PiiDetector` — маскирование email/SSN/phone.
- `AccessController` — RBAC по ролям.
- `RateLimiter` — ограничение запросов.

## Тестируемость
Есть позитивные, негативные и adversarial сценарии в `FinancialNewsOrchestratorTest`.

## Запуск
```bash
mvn test
mvn -q -DskipTests exec:java -Dexec.mainClass=com.example.finnews.App
```

## Идея для демо-видео (2–5 мин)
1. Показать запуск позитивного кейса (тикер + вопрос).
2. Показать блокировку вредоносного prompt.
3. Показать rate-limit fail.
4. Показать логи/метрики/цитирования в ответе.
5. Коротко показать как подменить mock MCP на реальный.

## Что улучшать дальше
- Подключить реальный MCP сервер (news/market APIs).
- Добавить embedding-модель и векторную БД (например, pgvector).
- Добавить HTTP API и UI dashboard.
- Добавить user feedback loop для оценки качества рекомендаций.
