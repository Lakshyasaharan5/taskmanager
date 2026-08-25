# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

- Run dev server: `./mvnw spring-boot:run`
- Build: `./mvnw clean install` (compile only: `./mvnw compile`)
- Run all tests: `./mvnw test`
- Run a single test class: `./mvnw test -Dtest=AiServiceTest`
- Run a single test method: `./mvnw test -Dtest=AiServiceTest#methodName`
- No linter/formatter is configured (no Checkstyle/Spotless/PMD in `pom.xml`).
- Requires Java 17. Set `OPENAI_API_KEY` env var for the AI suggestion endpoint to work (other AI-related env vars: `OPENAI_MODEL` default `gpt-4o`, `SAFETY_CHECK_MODEL` default `gpt-4o-mini`).
- H2 console: `http://localhost:8080/h2-console` while the app is running (`DB_USERNAME`/`DB_PASSWORD` env vars, default `admin`/`password`).
- A manual browser-based test UI for every API endpoint is served at `http://localhost:8080/` (`src/main/resources/static/index.html`, plain HTML/CSS/JS, no build step).

## Architecture

Single-module Spring Boot 4 app (Java 17, Spring AI 2.0.0, H2 in-memory DB), package root `com.eulerity.taskmanager`. One flat REST controller (not split per resource): `controller/TaskManagerController.java`, all routes under `/api`:

- Tasks: `POST/GET /api/tasks`, `GET/PUT/DELETE /api/tasks/{id}`, `GET /api/tasks/{id}/history` (audit trail), `POST /api/tasks/suggest` (AI suggestion — note the README says `/tasks/suggest`, the actual mapped path has the `/api` prefix like everything else)
- Projects: `POST/GET /api/projects`, `DELETE /api/projects/{id}`
- `GET /api/health`

Layers: `service/` → `repository/` (Spring Data JPA) → `entity/`. Requests/responses go through `dto/request/` and `dto/response/` — never expose entities directly from the controller. Filtering on `GET /api/tasks` (status/priority/dueBefore/dueAfter/projectId, sort, page/size) is built with `specification/TaskSpecification.java` (JPA Specification).

All error responses share one envelope built by `exception/GlobalExceptionHandler.java`: `{ "error": "ERROR_CODE", "message": "...", "fields": [...] }`. `fields` is populated only for validation errors; unexpected exceptions collapse to a generic internal-error response — upstream/AI error details are never leaked to the client, only logged.

**AI task-suggestion flow** (`service/AiService.java`, the one non-trivial piece — read this before touching AI behavior):
1. Sanitize raw query (strip control chars, collapse whitespace).
2. SHA-256 the sanitized query as a cache key; check `CacheService`/H2 `CacheEntity` first (cache hit skips both LLM calls, TTL via `ai.task-suggestion.cache-ttl-minutes`).
3. On cache miss, safety-check the query with the cheaper model (`ai.safety-check.model`) using `resources/prompts/safety-check-prompt.xml`; unsafe → rejected with `AiSuggestionException`.
4. If safe, call the main model (`ai.task-suggestion.model`) using `resources/prompts/task-suggestion-prompt.xml`, wrapped in a `CompletableFuture` with `ai.task-suggestion.timeout-seconds` timeout and up to 3 retry attempts.
5. Parse the model's JSON into `TaskRequestDto` and Bean-Validate it (same DTO used by task create/update); missing `dueDate` defaults to +7 days, `status` is forced to `TODO`.

Prompts are XML templates (not inline Java strings) so they stay structured and support dynamic `{today}`/`{userInput}` substitution — edit them in `resources/prompts/`, not in `AiService.java`.

**Task audit trail** (stretch feature, `service/AuditService.java` + `entity/TaskAuditLog.java`): every create/update/delete writes an audit row. `TaskAuditLog` stores `taskId` as a plain `Long` (not a JPA relation) so audit history survives task deletion. Field-level diffs on update are collected into a `List<FieldChange>` and stored as one serialized JSON column (`entity/converter/FieldChangeListConverter.java`) rather than one row per changed field — this list also makes update idempotent (no DB write if nothing changed). Audit failures never fail the parent request. `GET /api/tasks/{id}/history` returns an empty list for unknown/deleted ids rather than 404, since the trail should still be reachable after deletion.

## Design decisions worth knowing before changing behavior

(from `README.md` — read it for full rationale)

- New tasks always start at `status=TODO`; status must be changed via update. This is deliberate, to keep the audit trail's lifecycle consistent.
- Deleting a `Project` that still has associated tasks is blocked (409), not cascaded and not nulled — cascading felt dangerous, nulling felt like hiding the problem.
- Pagination is page/size based, not cursor-based; it does not yet handle shifting under concurrent mutation (`README.md` "Future" notes cursor-based-by-task-id as the planned fix — check there before assuming a fix is unplanned).
