# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project status

Etapp 0–2 from `docs/PROJECT_PLAN.md` are done: Flyway/datalager, Google OAuth2 auth (CORS/CSRF/global error handler), and external-recipe scraping (JSoup, SSRF guard, OpenGraph/JSON-LD extraction). Etapp 3 (reviews/feed) onward has not been started. There is still no `frontend/` — do not assume a `package.json` or Vue project exists until Etapp 5 creates it.

## What this is

`receptfeed`: a social recipe platform. Users paste an external recipe URL, the backend scrapes preview metadata (title/image/domain), and users write a rated review (1–5) shown in a public feed. Full details:

@docs/PROJECT_PLAN.md
@docs/ARCHITECTURE.md

## Stack

- Backend (`backend/`, scaffolded): Spring Boot **4.1.1**, Java 21, **Maven**. Dependencies: `spring-boot-starter-webmvc`, `spring-boot-starter-data-jpa`, `spring-boot-starter-security-oauth2-client`, `spring-boot-starter-flyway`, `spring-boot-starter-validation`, PostgreSQL driver, `org.jsoup:jsoup`.
- Frontend: Vue 3, **npm** (not pnpm/yarn) — not scaffolded yet, comes in Etapp 5.
- Local Postgres via `docker-compose.yml`, mapped to host port **5433**, not 5432 (another, unrelated container already used 5432 on this machine — don't "fix" this back to 5432).

### Spring Boot 4 / Spring Security 7 breaking changes (bit us once already, don't relearn this)

Most Spring tutorials/docs online still describe Boot 3 / Security 6. This project is on the newer major versions, which changed APIs we actually use:

- **Jackson 3**: groupId/package renamed from `com.fasterxml.jackson.*` to `tools.jackson.*`. `ObjectMapper` is `tools.jackson.databind.ObjectMapper`.
- **Spring Security 7**: `AntPathRequestMatcher` is gone. Use `org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher.pathPattern("/api/**")` instead (e.g. for `exceptionHandling().defaultAuthenticationEntryPointFor(...)`).
- **OAuth2 client starter artifact renamed**: it's `spring-boot-starter-security-oauth2-client` (and `-test`), not the old `spring-boot-starter-oauth2-client`.
- **CSRF cookie for a SPA needs two extra pieces**, or `CookieCsrfTokenRepository` alone silently does nothing useful:
  1. A filter that calls `csrfToken.getToken()` on every request (see `auth/CsrfCookieFilter.java`) — otherwise the token is resolved lazily and the `XSRF-TOKEN` cookie is never actually written.
  2. `.csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())` (the **plain**, non-XOR handler) — the default `XorCsrfTokenRequestAttributeHandler` masks the token for BREACH protection on server-rendered forms, which breaks a SPA that reads the raw cookie value and echoes it back in a header. Symptom if you regress this: `CsrfFilter` logs "Invalid CSRF token found" and every state-changing request 401s/403s even with a seemingly-matching token.
- **OIDC vs. plain OAuth2 login are different hooks.** Google's registration requests the `openid` scope, so Spring Security treats it as an **OIDC** login, not a plain OAuth2 one — `oauth2Login().userInfoEndpoint().userService(...)` (which takes an `OAuth2UserService<OAuth2UserRequest, OAuth2User>`) is silently never called. User auto-provisioning must be wired through `.oidcUserService(...)`, backed by a class that extends `OidcUserService`/overrides `loadUser(OidcUserRequest)` (see `auth/CustomOidcUserService.java`). A security review on this exact code caught it: login worked (a session was created) but no `User` row was ever provisioned, and `/api/auth/me` 401'd for every real login — this is easy to miss because the login flow *looks* like it works right up until something reads the local `User` row.

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
- The scraper must reject non-HTTP/HTTPS URLs, enforce timeout + response size limits, and validate the **resolved IP** (not just the hostname) against private/internal ranges for both IPv4 and IPv6 — re-check on every redirect hop to guard against DNS rebinding. This is an explicit security requirement, not an edge case to skip. Implemented in `recipe/SsrfGuard.java` + `recipe/SafeHtmlFetcher.java`.
  - Known residual gap, documented in `SsrfGuard`'s Javadoc: we validate the host via our own `InetAddress.getAllByName` lookup, then `HttpClient` re-resolves DNS independently a moment later to actually connect — a true DNS-rebinding attacker (controls the domain's authoritative DNS, flips the answer between those two resolutions) isn't fully closed. Closing it needs IP-pinned connections with manual TLS/SNI override, deliberately deferred as a hardening item, not a bug to "fix" reflexively.
- All HTTP/HTTPS domains are allowed in the MVP — there is no domain allowlist.
- Scraped metadata (`ExternalRecipe`) is cached by `source_url` (normalized: trimmed, fragment and known tracking params like `utm_*` stripped) and is not auto-refreshed in the MVP. `source_url` is the *original* normalized URL the user pasted; `title`/`imageUrl`/`domain` are derived from the *final* URL after following redirects (`RecipeScrapeService`) — so a shortlink's cache key is the shortlink, but its content/domain reflect where it actually points.
- `RecipeScrapeService` catches `DataIntegrityViolationException` on save and falls back to `findBySourceUrl` — two concurrent scrapes of a brand-new URL will race on the unique `source_url` constraint, and the loser should return the winner's row, not a 500.

## Not in MVP scope

Internal user-owned recipes, comment threads/followers, moderation, image proxy/cache, ingredient/instruction auto-import, local email/password login, non-Google OAuth providers, review deletion, recommendation algorithm — see `docs/PROJECT_PLAN.md` for the full list. Don't build toward these unless asked.
