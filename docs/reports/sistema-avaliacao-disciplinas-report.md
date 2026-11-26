# Implementation Report — Sistema de Avaliação de Disciplinas e Docentes (crUEL)

**Plan**: `./PLAN.md` (local, gitignored)   **Branch**: `feature/sistema-avaliacao-disciplinas`   **Status**: COMPLETE

## Summary
Full-stack application for anonymous course/teacher evaluation, built from an empty repository: a PostgreSQL schema in 3NF with
seed data and four analytical reports, a Spring Boot REST API that uses only JDBC + DAOs (no JPA), and a React SPA with the
Espresso (dark, default) / Latte (light) brown theme and Recharts reports. RF6 (one answer per student per evaluation,
without linking identity to answers) is enforced by the schema, by a single JDBC transaction, and by tests.

## Tasks completed
- Task 1 — `.gitignore` (UPDATE; kept the user's `.claude`/`PLAN.md` entries), `.env.example`, `.env`, `docker-compose.yml` (CREATE)
- Task 2 — `database/schema.sql` (CREATE): 11 tables, PK/FK/UNIQUE/CHECK/NOT NULL, indexes, idempotent drops
- Task 3 — `database/carga.sql` (CREATE): 27 users (2/4/21), 4 disciplines, 5 offers/evaluations (4 in 2026/2 + 1 in 2026/1),
  30 questions (15/10/5), 40 alternatives, 63 participations = 63 anonymous submissions, 999 answers; deliberate pending
  students and 3 unused questions
- Task 4 — `database/relatorios.sql` (CREATE): reports 1–4 (+ 3a/3b, 4a/4b/4c) with a coverage table of the 6 required SQL features
- Task 5 — `backend/` (CREATE): `pom.xml`, Maven wrapper, `application.properties`, config, model, dto, 6 DAOs, 4 services,
  7 controllers, exception handling, tests
- Task 6 — `frontend/` (CREATE): Vite/Tailwind/Recharts setup, theme + auth contexts, API client, Layout/ThemeToggle/ui
  components, the 7 pages listed in the plan
- Task 7 — `docs/DER.md`, `docs/DER.mmd`, `docs/DER.png`, `docs/MER_e_esquema_relacional.md`, `docs/prints/*.png`, `README.md` (CREATE/UPDATE)

## Tests added
- `backend/src/test/java/br/uel/avaliacao/service/RespostaServiceTest.java` — 12 tests using in-memory fake DAOs:
  participation and anonymous submission stored separately; second answer → 409 with no new submission; concurrent
  `DuplicateKeyException` → 409; different students can answer; outside the window rejected; non-enrolled → 403;
  non-student → 403; missing required question; alternative from another question; out-of-range score / wrong type;
  question not in the evaluation; blank optional free text ignored.
- `backend/src/test/java/br/uel/avaliacao/ArquiteturaTest.java` — 3 tests: no JPA/Hibernate on the classpath; the
  `SubmissaoAnonima`/`RespostaQuestao` records have no user field; the `submissao_anonima`/`resposta_questao` DDL in
  `schema.sql` never mentions `usuario`.
- Result: **15 run, 0 failures, 0 errors**.

Manual end-to-end checks (against a throwaway PostgreSQL 16 container):
- API: login 200/401; student → reports 403; no header → 401; student forging `X-User-Role: ADMIN` → 403; first
  submission 201, **second 409**, non-enrolled 403, missing required question 400, closed evaluation 400; DB counts
  stayed at participations = submissions.
- Transactions: an evaluation created with an invalid student id rolled back completely (0 evaluations, 0 orphan
  `avaliacao_questao` rows); a second evaluation for the same offer → 409; offer with a non-teacher → 400.
- UI (headless Edge over CDP): quick-access login → answer all 16 questions → success screen → reopening shows the lock →
  "Minhas avaliações" shows "Respondida"; reports updated immediately (Redes 10 → 11 submissions). Screenshots checked
  for both themes and at 390px mobile width.

## Validation results
- Level 1 (DB): `DROP/CREATE DATABASE` → `schema.sql`, `carga.sql`, `relatorios.sql` with `ON_ERROR_STOP=1` — **pass**
  (run via `docker exec` because `psql` is not installed on the host). `docker compose up` also loads schema + seed
  automatically (verified: 27 users, 5 evaluations, 30 questions, 63 submissions).
- Level 2 (backend): `mvn clean compile test-compile` **pass**; `mvn clean package -DskipTests` **pass**; `mvn test` **15/15**.
- Level 3 (frontend): `npm install && npm run build` **pass** (no chunk-size warning after splitting React/Recharts chunks).
- Chart palette: `validate_palette.js` **ALL CHECKS PASS** in light (on `#FFFFFF`) and dark (on `#2B1F18`).

## Deviations from the plan
1. **`submissao_anonima.id_submissao` is a random `UUID` (not `SERIAL`), and `data_submissao` / `data_participacao` are
   `DATE` (not `TIMESTAMP`).** With a serial key and timestamps, the order of submissions could be matched to the order
   of participations, which re-identifies students. That fails the plan's own test ("nem correlação de ordem trivial").
2. **`avaliacao.data_encerramento` was added.** The plan's single `data_avaliacao` + `hora_inicio`/`hora_fim` would limit
   an evaluation to one day. The window is now `[data_avaliacao hora_inicio, data_encerramento hora_fim]`, with
   `CHECK (data_encerramento >= data_avaliacao)`. `CHECK (hora_fim > hora_inicio)` is kept as specified.
3. **Spring Boot 4.1.1** (with `spring-boot-starter-webmvc`) instead of an unspecified 3.x: it was the version cached
   locally and is current. Java target stays 21. **Lombok and `dotenv-java` were left out**: the `.env` is loaded through
   `spring.config.import=optional:file:../.env[.properties]`, as the plan also allowed, and Lombok breaks on newer JDKs.
4. **Models and DTOs are Java `record`s** (immutable POJOs) instead of getter/setter classes.
5. **Extra files beyond the plan's list:** `config/AuthInterceptor.java`, `exception/RegraNegocioException.java`,
   `dto/CadastroDTOs.java`, `service/CatalogoService.java`, `service/RelatorioService.java`, `components/ui.jsx`,
   `App.jsx`, `main.jsx`, `vite.config.js`, `postcss.config.js`, `docs/DER.mmd`, `docs/prints/`. These keep the
   Controller → Service → DAO layering strict: no controller calls a DAO directly.
6. **Authorization trusts only `X-User-Id`.** The role is always read from the database, and `X-User-Role` (still sent by
   the frontend, as the plan describes) is ignored, so a student cannot pose as an admin by editing a header.
7. **`AvaliacaoDAO.listarPendentesPorAluno` became `listarDoAluno`.** It returns all of the student's evaluations with a
   `respondida` flag, because `MinhasAvaliacoesPage` has to show the answered ones as locked, not only the pending ones.
8. **The `RelatorioDAO` methods take a `hoje` date from the application clock** (America/Sao_Paulo) instead of SQL
   `CURRENT_DATE`, so the evaluation status agrees with the service's window check even when the DB runs in UTC.
9. **Extra report sections:** 3a/3b (best and most critical), 4b (pending students) and 4c (unused questions, which the
   plan's seed already anticipated), plus a viewer for anonymous comments.

## Issues encountered
- The host has no `psql` or `mvn` on PATH. Validation used a `postgres:16-alpine` container and the cached Maven 3.9.16;
  the repo ships `mvnw` so users don't need Maven installed.
- The PostgreSQL JDBC driver can't convert `int4` with `getObject(col, Long.class)`. Fixed with `getLong` + `wasNull`
  in `DisciplinaDAO`; the bug was found by the end-to-end API test.
- Hibernate Validator deprecates `@Valid` on a container. Moved to type-argument form (`List<@Valid X>`).
- The editor's `dbtools` SQL linter flags valid PostgreSQL syntax (`DROP … CASCADE`, `BEGIN;`, derived tables). These
  are false positives; every script runs with `ON_ERROR_STOP=1` on PostgreSQL 16.
- The source PDF (`Projeto — Banco de Dados I (Sistema de avaliações).pdf`) is not in the repository. Requirements were
  taken from the plan's restatement of RF1–RF7 and sections 5–7.
