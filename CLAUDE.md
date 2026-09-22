# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project status

This repo currently contains only planning docs (`docs/`) — no backend, frontend, or git history exists yet. Etapp 0 (Flyway + datalager) in `docs/PROJECT_PLAN.md` has not been started. Do not assume any `pom.xml`, `package.json`, or module layout exists until it's actually created.

## What this is

`receptfeed`: a social recipe platform. Users paste an external recipe URL, the backend scrapes preview metadata (title/image/domain), and users write a rated review (1–10) shown in a public feed. Full details:

@docs/PROJECT_PLAN.md
@docs/ARCHITECTURE.md

## Stack (decided, not yet scaffolded)

- Backend: Spring Boot, **Maven** (not Gradle), Spring Security, JSoup, Flyway, PostgreSQL.
- Frontend: Vue 3, **npm** (not pnpm/yarn).

## Rules easy to get wrong

- Controllers return DTOs only — never expose JPA entities directly, and `password_hash` must never appear in a JSON response.
- Flyway is the only owner of the schema after the first migration; don't rely on `ddl-auto=update` once it's verified (switch to `validate`).
- `rating` is an integer 1–10 (not 1–5).
- `POST /api/reviews` creates and returns `409 Conflict` if the user already has a review for that recipe; updates go through `PUT /api/reviews/{recipeId}`. A user can have at most one review per recipe — enforce with a unique `(user_id, recipe_id)` constraint (also used on `saved_recipe`).
- `POST /api/recipes/{id}/save` should behave idempotently (`200 OK` on repeat save), per the architecture doc's recommendation — not `409`.
- Auth uses server-side session/cookie + BCrypt (Spring Security), not JWT.
- The scraper must reject non-HTTP/HTTPS URLs, enforce timeout + response size limits, and must not follow redirects into private/internal IP ranges (SSRF protection) — this is a explicit security requirement, not an edge case to skip.
- All HTTP/HTTPS domains are allowed in the MVP — there is no domain allowlist.
- Scraped metadata (`ExternalRecipe`) is cached by `source_url` and is not auto-refreshed in the MVP.

## Not in MVP scope

Internal user-owned recipes, comment threads/followers, moderation, image proxy/cache, ingredient/instruction auto-import, OAuth, recommendation algorithm — see `docs/PROJECT_PLAN.md` for the full list. Don't build toward these unless asked.
