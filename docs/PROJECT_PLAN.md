# Projektplan: receptfeed

## Mål

Bygg en social receptplattform där användare kan klistra in en extern receptlänk, förhandsgranska det skrapade receptet, skriva en recension med betyg och kommentar samt se recensionen i ett socialt feed.

## MVP-scope

- Publika externa recept från URL.
- Feedet kräver inloggning för att läsas — en utloggad besökare möts av en inloggningssida (hero + "Logga in med Google") istället för feedet. (Ändrat efter MVP:ns ursprungliga beslut om ett publikt feed, för en tryggare känsla.)
- Inloggning sker endast via Google OAuth2 — inget lokalt lösenord finns i MVP:n.
- Inloggning krävs för att läsa feedet, skrapa (förhandsgranska) recept, skriva recensioner och spara recept.
- En användare kan skriva flera recensioner av samma externa recept över tid (t.ex. om receptet lagas igen). Varje recension är en egen post och kan redigeras individuellt. Tidigare recensioner av samma recept visas som referens innan en ny recension skrivs.
- Recensioner använder ett heltalsbetyg från 1 till 5 stjärnor.
- Feedet visar de senaste recensionerna.
- Användare kan spara recept till en privat "Ska laga"-lista.
- Endast metadata från externa sidor sparas: URL, titel, bild, domän och tidpunkt.
- Alla HTTP/HTTPS-domäner tillåts i MVP:n.
- Hämtad metadata refreshas inte automatiskt i MVP:n.
- Saknad receptbild ersätts av en stabil placeholder-bild.
- Receptets fullständiga instruktioner kopieras inte.
- Alla API-fel returneras i ett konsekvent JSON-format (status, felkod, användarvänligt meddelande) — aldrig stacktraces eller interna detaljer, och aldrig ett generiskt "något gick fel".

## Etapper

### Etapp 0: Förbered datalagret

- Lägg till Flyway och skapa versionshanterade PostgreSQL-migrationer.
- Byt från den nuvarande generiska `Recipe`-modellen till `ExternalRecipe` för externa URL:er.
- Lägg till miljövariabler för databasanslutning samt för Google OAuth2 (`GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`).
- Ta bort `ddl-auto=update` när första migrationen är verifierad.
- Bestäm hur befintliga demo-recept ska migreras eller rensas.

**Klart när:** applikationen kan starta mot en tom PostgreSQL-databas och Flyway skapar schemat reproducerbart.

### Etapp 1: Användare och autentisering (Google OAuth2)

- Skapa `User` med id, `oauth_subject` (Googles `sub`-claim, unik), email (unik) och display_name.
- Inget lösenord lagras — `spring-boot-starter-oauth2-client` hanterar inloggningen mot Google.
- Registrera en OAuth2-klient i Google Cloud Console (redirect URI för dev och prod) innan etappen påbörjas.
- Vid första inloggning skapas `User` automatiskt utifrån Googles profil (auto-provisionering) — separat registreringssteg behövs inte.
- Implementera `GET /api/auth/me` för aktuell användare (401 om ej inloggad) och logout.
- Använd server-side session/cookie efter OAuth2-inloggningen.
- Konfigurera CORS för Vue-frontendens origin (credentials tillåtna för sessionscookien).
- Konfigurera CSRF-skydd explicit — sessionsbaserad auth mot en separat frontend-origin kräver ett aktivt CSRF-beslut (t.ex. en cookie-baserad CSRF-token som frontend läser och skickar tillbaka), inte att skyddet stängs av.
- Implementera en global felhanterare (`@ControllerAdvice`) som mappar undantag till ett konsekvent JSON-felformat (status, felkod, meddelande) — se "Felhantering" i arkitekturdokumentet. Alla senare etapper återanvänder denna, ingen controller ska hantera fel på egen hand.

**Klart när:** en användare kan logga in med sitt Google-konto, autentiserade endpoints kan identifiera användaren, en session överlever mellan anrop utan att exponera tokens eller lösenord, och fel returneras i det gemensamma felformatet.

### Etapp 2: Externa recept och scraping

- Lägg till JSoup.
- Implementera `POST /api/recipes/scrape?url=...`, kräver inloggning (för att undvika att endpointen missbrukas som en öppen proxy för godtyckliga URL:er).
- Validera URL och begränsa protokoll till HTTP/HTTPS.
- Hämta titel och bild från OpenGraph eller Schema.org JSON-LD.
- Härled domän från URL (ta bort `www.`-prefix vid visning).
- Normalisera URL:en före lookup: trimning, borttagning av fragment och kända spårningsparametrar (t.ex. `utm_*`) innan matchning mot `source_url`.
- Återanvänd befintlig post via unikt `source_url`.
- Lägg in timeout, storleksgräns och tydliga fel.
- SSRF-skydd: validera den faktiskt uppslagna IP-adressen (inte bara hostnamnet) mot privata/interna ranges i både IPv4 och IPv6 direkt innan uppkoppling, och upprepa kontrollen vid varje redirect-steg för att skydda mot DNS-rebinding.

**Klart när:** samma URL returnerar samma `ExternalRecipe`, giltiga sajter ger titel/bild när metadata finns och ogiltiga eller otillgängliga URL:er ger kontrollerade 4xx-fel.

### Etapp 3: Recensioner och feed

- Skapa `Review` med user, recipe, rating, comment, created_at och updated_at.
- Validera `rating` som ett heltal mellan 1 och 5.
- Tillåt flera recensioner per `(user_id, recipe_id)` — ingen unik constraint på paret, varje recension är en egen post.
- Implementera `POST /api/reviews`, som alltid skapar en ny recension och returnerar `201 Created`.
- Implementera `PUT /api/reviews/{reviewId}` för att redigera en specifik egen recension (404 om den inte finns eller inte ägs av användaren).
- Implementera `GET /api/recipes/{recipeId}/reviews/mine` som returnerar den inloggade användarens tidigare recensioner av receptet — används för att visa historik innan en ny recension skrivs.
- Implementera `GET /api/feed` med senaste recensionerna först, med pagination från start (`page`/`size`) eftersom feedet är designat för att växa direkt.
- Returnera ett feed-DTO med användarnamn, receptets metadata, betyg, kommentar och tidpunkt.

**Klart när:** en inloggad användare kan recensera ett externt recept flera gånger, se sina tidigare recensioner av samma recept innan de skriver en ny, redigera en specifik egen recension och se recensionerna i feedet.

### Etapp 4: Ska laga-lista

- Skapa `SavedRecipe` med user, recipe och created_at.
- Lägg unik constraint på `(user_id, recipe_id)` — att spara ett recept är binärt, till skillnad från recensioner.
- Implementera `POST /api/recipes/{id}/save`.
- Lägg till GET och DELETE för användarens sparade recept.
- Visa sparstatus i feedet.

**Klart när:** en användare kan spara ett recept en gång, se sin lista och ta bort recept från listan.

### Etapp 5: Vue-frontend

- Inför enkel routing för `/feed` och `/add-review`.
- Bygg "Logga in med Google"/logout (ingen separat registrerings- eller inloggningsform).
- Bygg `/add-review`: URL → scrape-preview → visa egna tidigare recensioner av receptet (om några) → betyg (1–5 stjärnor) → kommentar → publicera.
- Bygg `/feed` med receptbild, titel, domän, användarnamn, stjärnor, kommentar och datum.
- Lägg till "Spara till min lista".
- Visa laddning, tomma listor, scrapingfel, authfel och publiceringsfel.
- Konfigurera API-bas-URL som miljövariabel så frontend kan peka mot rätt backend i dev/prod.
- Samla fetch-anrop i en liten API-klient när flera vyer delar logik.

**Klart när:** hela flödet kan genomföras i webbläsaren utan manuella API-anrop.

### Etapp 6: Testning och hårdning

- Enhetstesta URL-validering, metadataextraktion och SSRF-skyddet (inklusive DNS-rebinding-scenarier).
- Controller-testa OAuth2-inloggning, scraping, review (flera recensioner, redigering), feed och save-endpoints.
- Integrationstesta mot PostgreSQL, helst Testcontainers.
- Testa CORS- och CSRF-konfigurationen mot frontendens origin.
- Testa att samtliga felfall (400/401/404/409/422/502) returnerar det gemensamma felformatet med korrekt `code` och ett användarvänligt `message`.
- Testa dubletter i saved_recipe och behörighet.
- Testa timeout och ogiltiga externa URL:er.
- Testa frontendens huvudflöden och produktionsbygge.
- Uppdatera README med miljövariabler (inklusive Google OAuth2-klientuppgifter), Docker och testkommandon.

**Klart när:** hela testsviten (backend och frontend) körs grönt lokalt och i CI, och README beskriver hur en ny utvecklare kommer igång från noll.

## Prioriterad implementeringsordning

1. Flyway och schema.
2. Google OAuth2-klient (Google Cloud Console) och Spring Security-integration.
3. ExternalRecipe och JSoup-service.
4. Review (flera per recept) och feed.
5. SavedRecipe.
6. Vue-vyer och inloggning.
7. Integrationstester och säkerhetshårdning.

## Medvetet utanför första MVP:n

- Interna recept som ägs av användare.
- Kommentarstrådar och följare/kompisgraf.
- Moderering och rapportering.
- Bildproxy/cache.
- Automatisk import av ingredienser och instruktioner.
- Lokal inloggning med email/lösenord — endast Google OAuth2 stöds.
- Fler OAuth-leverantörer än Google.
- Radering av recensioner (endast skapa och uppdatera stöds).
- Rekommendationsalgoritm.
