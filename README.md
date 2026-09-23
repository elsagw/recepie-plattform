# receptfeed

En social receptplattform: klistra in en receptlänk, se en förhandsgranskning, recensera det och se recensionen i ett feed. Se `docs/PROJECT_PLAN.md` och `docs/ARCHITECTURE.md` för fulla detaljer.

## Du behöver

- Java 21
- Maven
- Node **^22.18.0** (använd `nvm use` i `frontend/`, se `frontend/README.md`)
- Docker (för lokal PostgreSQL)
- En Google OAuth2-klient (Client ID + Client secret) — se `docs/PROJECT_PLAN.md` för hur du skapar en

## Kom igång

### 1. Miljövariabler

```sh
cp .env.example .env
```

Fyll i `GOOGLE_CLIENT_ID` och `GOOGLE_CLIENT_SECRET` i `.env`.

### 2. Starta databasen

```sh
docker compose up -d postgres
```

Postgres körs på port **5433** (inte standard 5432).

### 3. Starta backend

```sh
cd backend
set -a
source ../.env   # eller sätt miljövariablerna på annat sätt
set +a
./mvnw spring-boot:run
```

Backend startar på `http://localhost:8080` och kör Flyway-migrationerna automatiskt mot den tomma databasen.

### 4. Starta frontend

```sh
cd frontend
nvm use
npm install
npm run dev
```

Frontend startar på `http://localhost:5173`.

### 5. Öppna appen

Gå till `http://localhost:5173` och logga in med Google.

## Testkommandon

```sh
cd backend && ./mvnw test    # backend-tester (kräver att Postgres körs)
cd frontend && npm run test:unit   # frontend-tester
cd frontend && npm run lint        # lint
cd frontend && npm run type-check  # TypeScript
```

## Struktur

- `backend/` — Spring Boot-API (Java 21, Maven)
- `frontend/` — Vue 3-frontend (TypeScript, Vite)
- `docs/` — projektplan och arkitekturdokumentation
- `docker-compose.yml` — lokal PostgreSQL


Lägg till:
spara rescept som man vill testa i framtiden? som ingen har lagt ut