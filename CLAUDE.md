# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project status

Etapp 0–5 from `docs/PROJECT_PLAN.md` are done: backend (Flyway/datalager, Google OAuth2 auth, external-recipe scraping with SSRF guard, reviews/feed, saved recipes) and now the Vue 3 + TypeScript frontend (`frontend/`) covering the full flow: login, `/feed` with save/unsave, `/add-review` with scrape-preview and review history. Etapp 6 (testing/hardening) has not been started.

Etapp 5 has been fully verified end-to-end in a real browser: Google login, scrape-preview, viewing past reviews of a recipe before submitting a new one, star-rating input, publishing, the published review appearing correctly in the feed, save/unsave toggling, logout, the `/add-review` login-gate for anonymous users, and a backend error (SSRF block) rendering correctly in the UI.

## What this is

`receptfeed`: a social recipe platform. Users paste an external recipe URL, the backend scrapes preview metadata (title/image/domain), and users write a rated review (1–5) shown in a feed. The feed requires a Google login to view (changed from the original MVP decision to make it public — see `docs/PROJECT_PLAN.md`/`docs/ARCHITECTURE.md`); logged-out visitors land on a login page instead. Full details:

@docs/PROJECT_PLAN.md
@docs/ARCHITECTURE.md

## Stack

- Backend (`backend/`, scaffolded): Spring Boot **4.1.1**, Java 21, **Maven**. Dependencies: `spring-boot-starter-webmvc`, `spring-boot-starter-data-jpa`, `spring-boot-starter-security-oauth2-client`, `spring-boot-starter-flyway`, `spring-boot-starter-validation`, PostgreSQL driver, `org.jsoup:jsoup`.
- Frontend (`frontend/`, scaffolded via `create-vue`): Vue 3, TypeScript, Vue Router, ESLint (+oxlint), Vitest. **No Pinia** — auth state is a module-level singleton in `composables/useAuth.ts`; the app is small enough that a store would be pure ceremony. **npm** (not pnpm/yarn).
  - **Requires Node ^22.18.0**, unlike the rest of the stack (backend/tooling assumes Node 20). Use `nvm use` in `frontend/` (reads `.nvmrc`, pinned to `22.23.2`, already installed via nvm on this machine). Running `npm install`/`npm run *` under Node 20 will either fail outright or hit a confusing `npm error Cannot read properties of null (reading 'edgesOut')` from `@npmcli/arborist` — that specific error was actually an old-npm bug (fixed by `npm install -g npm@latest`), not a Node-version error, so don't assume Node is the only thing that can cause it.
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
- **`response.getWriter()` in a hand-written security component (`AuthenticationEntryPoint`, `AccessDeniedHandler`, any raw servlet filter) does not reliably give you UTF-8** — writing JSON with å/ä/ö through it can silently mangle those bytes (confirmed: the byte for "Å" decoded as U+FFFD in the browser). A normal `@RestControllerAdvice`/`ResponseEntity` response doesn't have this problem, Spring's message converters handle the encoding correctly — it's specifically the manual `response.getWriter()` + `objectMapper.writeValue(...)` pattern used outside the MVC dispatch path (`ApiAuthenticationEntryPoint`, `ApiAccessDeniedHandler`) that's affected. Fix: write to `response.getOutputStream()` instead — `ObjectMapper.writeValue(OutputStream, ...)` always writes UTF-8, no ambiguity. Any future hand-written security component that writes JSON directly must do the same.

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
- Feed **ordering/position** uses `created_at` only — editing a review does *not* bump it to the top of the feed (deliberate: prevents gaming visibility by repeatedly re-editing). Both `GET /api/feed` and `GET /api/recipes/{id}/reviews/mine` still *display* `updatedAt` alongside `createdAt` so the frontend can show an "edited" indicator; just don't use `updatedAt` for sorting.
- `saved_recipes` mirrors the `RecipeScrapeService` concurrency pattern: `SavedRecipeService` catches `DataIntegrityViolationException` on the unique `(user_id, recipe_id)` constraint and returns the existing row instead of erroring, so two concurrent "save" clicks don't 500. `DELETE /api/recipes/{id}/save` is unconditionally idempotent (`204` whether or not it was saved) — don't add a 404-if-not-saved check.
- `GET /api/feed` now requires login (changed from the original public-feed MVP decision — see `docs/PROJECT_PLAN.md`/`docs/ARCHITECTURE.md`). `savedByCurrentUser` is still resolved via `CurrentUserResolver.resolveCurrentUser(principal)` (the non-throwing variant) rather than the throwing one, since Spring Security's `.anyRequest().authenticated()` already guarantees a non-null principal by the time the controller runs — the non-throwing call is just defensive, not load-bearing.
- Frontend: `/feed` (the app's effective home route, since `/` redirects there) shows a login landing page — hero headline + "Logga in med Google" button — when logged out, and only fetches/shows the feed once `useAuth()`'s `currentUser` resolves truthy. Implemented inline in `FeedView.vue` (same per-view gating convention as `/add-review`), not as a separate route/component.
- All `/api/**` errors go through the shared JSON shape even when Spring Security's filter chain rejects a request before it reaches a controller — not just `GlobalExceptionHandler`. `ApiAuthenticationEntryPoint` covers anonymous 401s, `ApiAccessDeniedHandler` covers authenticated-but-denied cases (most commonly a missing/invalid CSRF token, which can surface as 401 or 403 depending on Spring Security's internal exception routing — both paths now return the same JSON shape regardless). Found by manually testing the CSRF-rejection path, not by the security-review agent.

- Frontend: `POST /api/recipes/scrape` is a POST despite reading like a query lookup (it may write a new `ExternalRecipe` row) — always call it through `api.post`, never `api.get`, or the CSRF header won't be sent and Spring Security will reject it.
- Frontend: every fetch goes through `src/api/client.ts`'s `api` object, which handles `credentials: 'include'` and the `X-XSRF-TOKEN` header for non-GET requests automatically. Don't call `fetch()` directly in a component — you'll silently lose CSRF handling and cross-origin cookies.
- Frontend `/add-review` gates its *entire* view behind login (not just the publish step) — scraping itself requires auth per the backend's `POST /api/recipes/scrape` rule above, so there's no anonymous preview-then-login flow.

## Not in MVP scope

Internal user-owned recipes, comment threads/followers, moderation, image proxy/cache, ingredient/instruction auto-import, local email/password login, non-Google OAuth providers, review deletion, recommendation algorithm — see `docs/PROJECT_PLAN.md` for the full list. Don't build toward these unless asked.
