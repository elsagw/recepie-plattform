# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project status

This repo currently contains only planning docs (`docs/`) — no backend, frontend, or git history exists yet. Etapp 0 (Flyway + datalager) in `docs/PROJECT_PLAN.md` has not been started. Do not assume any `pom.xml`, `package.json`, or module layout exists until it's actually created.

## What this is

`receptfeed`: a social recipe platform. Users paste an external recipe URL, the backend scrapes preview metadata (title/image/domain), and users write a rated review (1–5) shown in a public feed. Full details:

@docs/PROJECT_PLAN.md
@docs/ARCHITECTURE.md

## Stack (decided, not yet scaffolded)

- Backend: Spring Boot, **Maven** (not Gradle), Spring Security OAuth2 Client, JSoup, Flyway, PostgreSQL.
- Frontend: Vue 3, **npm** (not pnpm/yarn).

## Rules easy to get wrong

- Controllers return DTOs only — never expose JPA entities directly.
- Auth is **Google OAuth2 only** — no username/password, no BCrypt, no registration form, no `password_hash` field anywhere. Users are auto-provisioned on first login from the Google profile; `oauth_subject` (Google's `sub` claim) is the real identity key, not `email`.
- Flyway is the only owner of the schema after the first migration; don't rely on `ddl-auto=update` once it's verified (switch to `validate`).
- `rating` is an integer 1–5 (not 1–10).
- `POST /api/reviews` **always creates a new review** and returns `201 Created` — multiple reviews per `(user, recipe)` pair are allowed by design (e.g. re-cooking the same recipe). There is no unique `(user_id, recipe_id)` constraint on `review` and no `409 Conflict` rule. Editing goes through `PUT /api/reviews/{reviewId}`, scoped by the review's own id — not by `recipeId`.
- `GET /api/recipes/{recipeId}/reviews/mine` returns the current user's past reviews of a recipe; the add-review flow shows these before the user submits a new one.
- `saved_recipe` still has a unique `(user_id, recipe_id)` constraint — saving is binary, unlike reviews. `POST /api/recipes/{id}/save` should behave idempotently (`200 OK` on repeat save), not `409`.
- CORS must allow only the frontend's own origin(s) with `credentials: true`. CSRF must be actively configured (e.g. `CookieCsrfTokenRepository`) since this is session-cookie auth across a separate frontend origin — never assume Spring Security's defaults are already correct here.
- All error responses go through one global `@ControllerAdvice`/`GlobalExceptionHandler` and share one JSON shape: `{status, code, message, path, timestamp}`. `message` must always be safe to show a user — never a stack trace or raw exception text; log internals server-side instead. Don't hand-roll error bodies per controller.
- `POST /api/recipes/scrape` requires login (prevents the endpoint being used as an open URL-fetch proxy).
- The scraper must reject non-HTTP/HTTPS URLs, enforce timeout + response size limits, and validate the **resolved IP** (not just the hostname) against private/internal ranges for both IPv4 and IPv6 — re-check on every redirect hop to guard against DNS rebinding. This is an explicit security requirement, not an edge case to skip.
- All HTTP/HTTPS domains are allowed in the MVP — there is no domain allowlist.
- Scraped metadata (`ExternalRecipe`) is cached by `source_url` (normalized: trimmed, fragment and known tracking params like `utm_*` stripped) and is not auto-refreshed in the MVP.

## Not in MVP scope

Internal user-owned recipes, comment threads/followers, moderation, image proxy/cache, ingredient/instruction auto-import, local email/password login, non-Google OAuth providers, review deletion, recommendation algorithm — see `docs/PROJECT_PLAN.md` for the full list. Don't build toward these unless asked.
