# Kompetensplan: vad som redan finns och vad som saknas

Det här dokumentet spårar var `receptfeed`-projektet står i förhållande till fem tekniska
kompetensområden (databashantering, automatiserade tester, containerbaserad teknik, Java,
Vue/React), och vad som konkret behöver byggas för att erfarenheten ska vara äkta och
beskrivbar — inte bara "det finns en Dockerfile".

## Statusöversikt

| Område | Status | Underlag i projektet |
|---|---|---|
| Databashantering | ✅ Stark, redan verifierad | PostgreSQL, 7 versionshanterade Flyway-migreringar, JPA/Hibernate, unika constraints (`source_url`, `(user_id, recipe_id)`), medveten hantering av race conditions (`DataIntegrityViolationException`-fallback i `RecipeScrapeService`/`SavedRecipeService`) |
| Java | ✅ Stark, redan verifierad | Hela backend: Spring Boot 4.1.1 på Java 25, Spring Security/OAuth2, JPA, global felhantering |
| Vue/React | ✅ Vue-erfarenhet, redan verifierad | Vue 3 + TypeScript-frontend, hela flödet (login → scrape → review → feed → save) klart och testat i webbläsare |
| Automatiserade tester | ⚠️ Delvis — grunden finns, Etapp 6 inte klar | 42 enhetstester finns (`SsrfGuardTest`, `UrlNormalizerTest`, `RecipeMetadataExtractorTest`, `DomainExtractorTest`, `ImageStorageServiceTest`, `ReviewServiceTest`), men inga controller-tester, inga integrationstester mot riktig databas, inga CORS/CSRF-tester, inga frontend-tester |
| Containerbaserad teknik | ✅ Stark, redan verifierad | Multi-stage `Dockerfile` för backend (Maven-byggsteg → slim JRE 25-image, icke-root-användare) och frontend (Node-byggsteg → nginx), `docker-compose.yml` kör hela stacken (Postgres + backend + frontend) med healthcheck-styrd startordning och en namngiven volym för uppladdade bilder |

## Plan A — Automatiserade tester (täcker Etapp 6 i `docs/PROJECT_PLAN.md`)

- [x] Controller-tester (`@WebMvcTest`) för `AuthController`, `RecipeScrapeController`,
      `ReviewController`, `FeedController`, `SavedRecipeController` — statuskoder och
      JSON-kontrakt, mockad service-lager (20 nya tester, `addFilters = false` så
      säkerhetsfiltren inte stör de rena controller-testerna)
- [x] `SecurityConfigTest`: OAuth2-inloggningsflöde (redirect till Google), CORS-headers
      (tillåten vs. avvisad origin), CSRF-token-flöde (cookie sätts på varje request,
      state-ändrande request utan token avvisas, med giltig token accepteras) — 7 tester
      mot den riktiga filterkedjan
- [x] Integrationstester mot riktig PostgreSQL via **Testcontainers**
      (`RepositoryIntegrationTest`, 4 tester): unika constraints (`source_url`,
      `(user_id, recipe_id)`), flera recensioner per user/recept tillåtet, feedets
      paginering och `createdAt desc`-ordning — körs mot en engångscontainer, oberoende av
      `docker compose up`
- [x] Felfallstester (`GlobalExceptionHandlerTest`, 10 tester): samtliga statuskoder
      (400/401/404/409/422/502) samt Springs egna ramverksundantag mappar till det
      gemensamma felformatet, och interna undantagsmeddelanden läcker aldrig ut
- [x] `RecipeScrapeServiceTest` (6 tester): cache-hit (ingen fetch), ny URL (hämta+spara),
      race-villkor vid samtidig scrape, SSRF-avvisning och timeout propagerar som
      `ApiException`
- [x] Frontend (Vitest, 16 tester i 4 filer): `useAuth` (inloggning/utloggning/401-hantering),
      `api/client` (CSRF-header, credentials, felhantering, multipart), `FeedView`
      (login-gate, feed-rendering, save/unsave) och `AddReviewView` (login-gate,
      scrape-preview + historik, publicera recension + navigering)

**Status: Plan A klar.** Backend 100/100 tester gröna (`./mvnw test`, upp från 42 vid
start). Frontend 16/16 tester gröna (`npm run test:unit`), `type-check` och `lint` rena.

Under arbetet hittades och fixades ett separat, obesläktat problem: `frontend/node_modules`
hade fel CPU-arkitektur (x64-bindningar på denna arm64-Mac, troligen kvar från en tidigare
körning under Rosetta) vilket fick Vitest att krascha direkt vid start. Löst med `npm install`
efter att arm64-Node var aktiv (`nvm use`); `package-lock.json` fick bara mindre
metadatakorrigeringar, inga versionsändringar.

## Plan B — Containerisering

- [x] `Dockerfile` för backend (`backend/Dockerfile`): multi-stage build — steg 1 bygger
      jar:en med `eclipse-temurin:25-jdk` + Maven wrapper (med cachat dependency-lager),
      steg 2 kör den på `eclipse-temurin:25-jre` som en icke-root-användare
- [x] `Dockerfile` för frontend (`frontend/Dockerfile`): steg 1 bygger statiska filer med
      `node:22-alpine` (`npm run build`, inklusive type-check), steg 2 serverar dem med
      `nginx:alpine` + en `nginx.conf` med SPA-fallback (`try_files ... /index.html`) så
      Vue Routers `/feed`-style URL:er funkar på direktladdning
- [x] `docker-compose.yml` utökad med `backend`- och `frontend`-services. Inget manuellt
      nätverk behövdes — Compose kopplar ihop services i samma fil automatiskt via
      tjänstenamn (`postgres`, port 5432 internt). En healthcheck på Postgres gör att
      backend väntar in en riktigt redo databas innan den startar
- [x] Miljövariabler (Google-uppgifter, DB-anslutning) läses från `${VAR}` i
      `docker-compose.yml`, som Compose fyller i automatiskt från `.env` — inget
      hårdkodat i compose-filen
- [x] Verifierat med `docker compose up -d --build`: alla tre containrar startar,
      `GET /api/auth/me` ger `401` med rätt felformat, Flyway migrerar mot
      `postgres:5432` (det interna nätverksnamnet), frontend serverar `index.html` även
      på ett direktladdat `/feed`
- [x] Dokumenterat i `README.md` under "Kör hela stacken med Docker"

**Status: Plan B klar.** Alla checkboxar verifierade med en riktig `docker compose up`, inte
bara att Dockerfiles skrevs.

**Bra att kunna berätta om i en intervju:** varför `VITE_API_BASE_URL` måste vara ett
build-arg och inte en vanlig miljövariabel (Vite bakar in den i JS-filerna vid bygget,
inte vid körning — statiska filer har ingen serverkod som kan läsa env vars senare), och
varför frontend-imagen pekar på `localhost:8080` för backend medan backend-imagen pekar
på `postgres:5432` för databasen (webbläsaren respektive backend-containern gör anropen
från olika nätverk).

## Nästa steg

Både Plan A och Plan B är klara. Kvar om du vill gå längre: CI-wiring (köra testsviten
automatiskt i GitHub Actions vid varje push) och att publicera images till ett register.
