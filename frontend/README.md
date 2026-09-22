# receptfeed frontend

Vue 3 + TypeScript + Vite. Talks to the Spring Boot backend in `../backend`.

## Requirements

- Node **^22.18.0** (the scaffolding tooling requires it; `nvm use` picks up `.nvmrc`). The rest of this repo's docs/CLAUDE.md assume Node 20 elsewhere in the stack — the frontend is the exception.

## Setup

```sh
nvm use
npm install
cp .env.example .env   # only if you need a non-default VITE_API_BASE_URL
npm run dev
```

The backend must be running (see `../backend`) and its `app.frontend-origin` must match this dev server's origin (`http://localhost:5173` by default) for CORS/OAuth2 redirects to work.

## Commands

```sh
npm run dev          # dev server with hot reload
npm run build         # type-check + production build
npm run test:unit      # Vitest
npm run lint           # oxlint + eslint, with --fix
npm run type-check     # vue-tsc only
```

## Recommended IDE Setup

[VS Code](https://code.visualstudio.com/) + [Vue (Official)](https://marketplace.visualstudio.com/items?itemName=Vue.volar) (and disable Vetur).
