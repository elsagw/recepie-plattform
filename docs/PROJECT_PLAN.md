# Projektplan: receptfeed

## Mål

Bygg en social receptplattform där användare kan klistra in en extern receptlänk, förhandsgranska det skrapade receptet, skriva en recension med betyg och kommentar samt se recensionen i ett socialt feed.

## MVP-scope

- Publika externa recept från URL.
- Feedet är publikt och kan läsas utan konto.
- Inloggning krävs för att skapa recensioner och spara recept.
- En användare kan ha högst en recension per recept och kan uppdatera den.
- Recensioner använder ett heltalsbetyg från 1 till 10 stjärnor.
- Feedet visar de senaste recensionerna.
- Användare kan spara recept till en privat "Ska laga"-lista.
- Endast metadata från externa sidor sparas: URL, titel, bild, domän och tidpunkt.
- Alla HTTP/HTTPS-domäner tillåts i MVP:n.
- Hämtad metadata refreshas inte automatiskt i MVP:n.
- Saknad receptbild ersätts av en stabil placeholder-bild.
- Receptets fullständiga instruktioner kopieras inte.

## Etapper

### Etapp 0: Förbered datalagret

- Lägg till Flyway och skapa versionshanterade PostgreSQL-migrationer.
- Byt från den nuvarande generiska `Recipe`-modellen till `ExternalRecipe` för externa URL:er.
- Lägg till miljövariabler för databasanslutning.
- Ta bort `ddl-auto=update` när första migrationen är verifierad.
- Bestäm hur befintliga demo-recept ska migreras eller rensas.

**Klart när:** applikationen kan starta mot en tom PostgreSQL-databas och Flyway skapar schemat reproducerbart.

### Etapp 1: Användare och autentisering

- Skapa `User` med id, username, email och password_hash.
- Lägg till unik constraint för username och email.
- Hasha lösenord med BCrypt via Spring Security.
- Implementera registrering, login, logout och aktuell användare.
- Använd server-side session/cookie i första versionen.

**Klart när:** en användare kan registrera sig, logga in och autentiserade endpoints kan identifiera användaren utan att lösenord exponeras.

### Etapp 2: Externa recept och scraping

- Lägg till JSoup.
- Implementera `POST /api/recipes/scrape?url=...`.
- Validera URL och begränsa protokoll till HTTP/HTTPS.
- Hämta titel och bild från OpenGraph eller Schema.org JSON-LD.
- Härled domän från URL.
- Återanvänd befintlig post via unikt `source_url`.
- Lägg in timeout, storleksgräns, tydliga fel och skydd mot privata nätverksadresser.

**Klart när:** samma URL returnerar samma `ExternalRecipe`, giltiga sajter ger titel/bild när metadata finns och ogiltiga eller otillgängliga URL:er ger kontrollerade 4xx-fel.

### Etapp 3: Recensioner och feed

- Skapa `Review` med user, recipe, rating, comment och created_at.
- Validera `rating` som ett heltal mellan 1 och 10.
- Lägg unik constraint på `(user_id, recipe_id)`.
- Implementera `POST /api/reviews`.
- Implementera `PUT /api/reviews/{recipeId}` för uppdatering av användarens befintliga recension.
- Returnera `409 Conflict` från POST om användaren redan har en recension.
- Implementera `GET /api/feed` med senaste recensionerna först.
- Returnera ett feed-DTO med användarnamn, receptets metadata, betyg, kommentar och tidpunkt.
- Lägg pagination innan feedet växer.

**Klart när:** en inloggad användare kan recensera ett externt recept en gång, uppdatera recensionen och se den i feedet.

### Etapp 4: Ska laga-lista

- Skapa `SavedRecipe` med user, recipe och created_at.
- Lägg unik constraint på `(user_id, recipe_id)`.
- Implementera `POST /api/recipes/{id}/save`.
- Lägg till GET och DELETE för användarens sparade recept.
- Visa sparstatus i feedet.

**Klart när:** en användare kan spara ett recept en gång, se sin lista och ta bort recept från listan.

### Etapp 5: Vue-frontend

- Inför enkel routing för `/feed` och `/add-review`.
- Bygg login/register/logout.
- Bygg `/add-review`: URL → scrape-preview → rating → kommentar → publicera.
- Bygg `/feed` med receptbild, titel, domän, användarnamn, stjärnor, kommentar och datum.
- Lägg till "Spara till min lista".
- Visa laddning, tomma listor, scrapingfel, authfel och publiceringsfel.
- Kontrollera om användaren redan har recenserat receptet och välj POST eller PUT därefter.
- Samla fetch-anrop i en liten API-klient när flera vyer delar logik.

**Klart när:** hela flödet kan genomföras i webbläsaren utan manuella API-anrop.

### Etapp 6: Testning och hårdning

- Enhetstesta URL-validering och metadataextraktion.
- Controller-testa auth, scraping, review, feed och save-endpoints.
- Integrationstesta mot PostgreSQL, helst Testcontainers.
- Testa dubletter och behörighet.
- Testa timeout och ogiltiga externa URL:er.
- Testa frontendens huvudflöden och produktionsbygge.
- Uppdatera README med miljövariabler, Docker och testkommandon.

## Prioriterad implementeringsordning

1. Flyway och schema.
2. User och Spring Security.
3. ExternalRecipe och JSoup-service.
4. Review och feed.
5. SavedRecipe.
6. Vue-vyer och auth.
7. Integrationstester och säkerhetshårdning.

## Medvetet utanför första MVP:n

- Interna recept som ägs av användare.
- Kommentarstrådar och följare/kompisgraf.
- Moderering och rapportering.
- Bildproxy/cache.
- Automatisk import av ingredienser och instruktioner.
- OAuth och e-postverifiering.
- Rekommendationsalgoritm.
