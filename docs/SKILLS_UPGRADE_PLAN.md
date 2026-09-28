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
| Containerbaserad teknik | ⚠️ Delvis — bara databasen körs containeriserad | `docker-compose.yml` kör Postgres i container, men själva applikationen (backend/frontend) har ingen `Dockerfile` och körs inte containeriserad alls |

De två understa raderna är det som behöver byggas ut för att kunna svara "Ja" med ett
ärligt, konkret svar i en jobbansökan.

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

- [ ] `Dockerfile` för backend: multi-stage build (Maven-byggsteg → slim JRE 25
      runtime-image)
- [ ] `Dockerfile` för frontend: Node-byggsteg (`npm run build`) → statisk servering
      (t.ex. nginx)
- [ ] Utöka `docker-compose.yml` med `backend`- och `frontend`-services, nätverk mellan
      dem och `postgres`-servicen
- [ ] Miljövariabler (Google OAuth2-uppgifter, DB-anslutning) via `.env`/`env_file`,
      inte hårdkodat i compose-filen
- [ ] Verifiera att hela stacken startar reproducerbart med ett enda `docker compose up`
- [ ] Dokumentera i `README.md` hur man kör hela stacken containeriserad

**Startpunkt:** backend-`Dockerfile` först — den är mer relevant för Java-frågan i
ansökan och enklare att verifiera (ett `docker build` + `docker run` mot befintlig
`docker-compose`-databas).

## Nästa steg

Välj Plan A, Plan B, eller båda (i så fall A → B, eftersom automatiserade tester väger
tyngre i de flesta jobbannonser och är mer omfattande att bygga klart).
