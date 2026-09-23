---
name: receptfeed-ui
description: Project-specific conventions for editing or improving the receptfeed Vue 3 frontend (frontend/) — component structure, styling tokens, state/loading/error patterns. Use for any UI/frontend work in this repo (new views, component tweaks, visual polish, "förbättra UI", "gör snyggare", add-review/feed/saved/friends views). Pairs with the general frontend-design skill for visual-design judgment.
---

# receptfeed frontend conventions

Grounding for UI work in `frontend/`. This skill covers *this repo's* existing patterns — for
general visual-design taste (typography, color, layout judgment), also use the `frontend-design`
skill; use both together when doing anything visually significant.

## Stack facts (see also root CLAUDE.md)

- Vue 3 + `<script setup lang="ts">`, Vue Router, no Pinia — auth state lives in the
  `useAuth()` singleton (`src/composables/useAuth.ts`).
- Views live in `src/views/*View.vue`, routed from `src/router/index.ts`. Shared UI pieces go in
  `src/components/`.
- All backend calls go through `src/api/client.ts`'s `api` object (`api.get`/`api.post`/`api.delete`),
  never raw `fetch()` — it handles credentials + CSRF header. Catch errors as `ApiError` and fall
  back to a Swedish default message.
- Everything user-facing is **in Swedish** (labels, empty states, error messages). Match the
  existing tone: short, plain, no exclamation marks.

## Styling conventions actually in use

- Plain scoped `<style scoped>` blocks per component/view — no CSS framework, no Tailwind, no
  CSS-in-JS. Keep new components consistent with this (don't introduce a styling system
  mid-project).
- Design tokens are CSS custom properties defined in `src/assets/base.css` and `src/assets/main.css`:
  `--color-background`, `--color-background-soft`, `--color-background-mute`, `--color-border`,
  `--color-border-hover`, `--color-heading`, `--color-text`, `--color-accent`,
  `--color-accent-soft`, `--color-secondary`, `--color-danger`, `--color-star-filled`,
  `--color-star-empty`. Reuse these instead of hardcoding new colors; add a new token to `main.css`
  if a genuinely new semantic color is needed, rather than inlining a hex value in a component.
- Dark mode is handled via `@media (prefers-color-scheme: dark)` redefining those same tokens in
  `base.css`/`main.css` — there's no manual light/dark toggle. Any new color must be added to both
  the `:root` block and the dark-mode block.
- Deliberate warm, food-inspired palette (chosen for "mysigt" — cozy — over the original unmodified
  `create-vue` scaffold green): cream/espresso backgrounds (`--rf-cream*`/`--rf-espresso*` primitives
  in `base.css`), a terracotta/paprika primary (`--color-accent`, `#b5502e` light / `#cb6b44` dark),
  a herb-green secondary (`--color-secondary`, `#4a6741` light / `#6e9160` dark) reserved for
  confirmation/positive states (saved badge, success messages) rather than primary CTAs, and the
  existing gold star color (`--color-star-filled`, unchanged) used only for ratings. Dark-mode accent/
  secondary/danger values are deliberately *lighter* than their light-mode counterparts, not the same
  value — a single shade can't hit good contrast both as text-on-dark-background and as
  white-text-on-filled-button at once, so don't "simplify" by reusing the light-mode hex in dark mode.

## UI state pattern used in every view (follow it, don't reinvent)

Every data-fetching view (`FeedView.vue`, `AddReviewView.vue`, `SavedView.vue`, …) follows the same
shape — match it in new views/components:

```
const isLoading = ref(true)
const loadError = ref<string | null>(null)
// ... fetch in a named async function, set isLoading/loadError around it
```

```html
<p v-if="isLoading" class="state">Laddar …</p>
<div v-else-if="loadError" class="state error">
  <p>{{ loadError }}</p>
  <button type="button" @click="retry">Försök igen</button>
</div>
<p v-else-if="items.length === 0" class="state">…tomt-läge…</p>
<template v-else>…content…</template>
```

`.state` / `.state.error` (using `var(--color-danger)`) are the conventional classes for these three
states — reuse them rather than inventing new loading/error/empty treatments per view.

## Login-gating

Views/actions that require auth check `useAuth().currentUser` and either hide the control or show
a login prompt — see `App.vue`'s conditional `RouterLink`s and `AddReviewView.vue` gating its whole
view behind login. Follow the existing per-view gating style rather than adding a new route guard
pattern, unless asked to refactor that deliberately.

## Before finishing UI work

Per root CLAUDE.md: start the dev server (`frontend/`, `nvm use` first — Node ^22.18.0 required) and
click through the actual change in a browser (or via the `claude-in-chrome` tools) before reporting
it done. Type-checking passing is not the same as the UI working.
