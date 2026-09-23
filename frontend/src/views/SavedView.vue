<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { api, ApiError, resolveImageUrl } from '@/api/client'
import type { SavedRecipe } from '@/types/api'
import LoadingSpinner from '@/components/LoadingSpinner.vue'

const items = ref<SavedRecipe[]>([])
const isLoading = ref(true)
const loadError = ref<string | null>(null)
const removePendingFor = ref<number | null>(null)

async function load() {
  isLoading.value = true
  loadError.value = null
  try {
    items.value = await api.get<SavedRecipe[]>('/api/saved-recipes')
  } catch (error) {
    loadError.value = error instanceof ApiError ? error.message : 'Kunde inte hämta sparade recept.'
  } finally {
    isLoading.value = false
  }
}

async function remove(recipeId: number) {
  removePendingFor.value = recipeId
  try {
    await api.delete(`/api/recipes/${recipeId}/save`)
    items.value = items.value.filter((item) => item.recipeId !== recipeId)
  } catch (error) {
    loadError.value = error instanceof ApiError ? error.message : 'Kunde inte ta bort receptet.'
  } finally {
    removePendingFor.value = null
  }
}

function formatDate(iso: string): string {
  return new Date(iso).toLocaleDateString('sv-SE', { year: 'numeric', month: 'short', day: 'numeric' })
}

onMounted(load)
</script>

<template>
  <section>
    <h1>Ska laga</h1>

    <div v-if="isLoading" class="state"><LoadingSpinner /></div>

    <div v-else-if="loadError" class="state error">
      <p>{{ loadError }}</p>
      <button type="button" @click="load">Försök igen</button>
    </div>

    <p v-else-if="items.length === 0" class="state">
      Du har inte sparat några recept än. <RouterLink to="/feed">Bläddra i feedet</RouterLink> och spara något.
    </p>

    <ul v-else class="grid">
      <li v-for="item in items" :key="item.recipeId" class="tile">
        <a
          :href="item.sourceUrl"
          target="_blank"
          rel="noopener noreferrer"
          class="thumb"
          :class="{ placeholder: !item.imageUrl }"
        >
          <img v-if="item.imageUrl" :src="resolveImageUrl(item.imageUrl) ?? ''" :alt="item.title ?? ''" />
          <span v-else>{{ item.domain }}</span>
        </a>
        <div class="tile-info">
          <strong class="title">{{ item.title ?? 'Recept utan titel' }}</strong>
          <div class="meta">{{ item.domain }} · sparad {{ formatDate(item.savedAt) }}</div>
          <div class="tile-actions">
            <RouterLink :to="{ path: '/add-review', query: { url: item.sourceUrl } }" class="review-button">
              Recensera
            </RouterLink>
            <button
              type="button"
              class="delete-button"
              :disabled="removePendingFor === item.recipeId"
              title="Ta bort från Ska laga-listan"
              aria-label="Ta bort från Ska laga-listan"
              @click="remove(item.recipeId)"
            >
              <svg viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <polyline points="3 6 5 6 21 6"></polyline>
                <path d="M19 6l-1 14a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2L5 6" />
                <path d="M10 11v6" />
                <path d="M14 11v6" />
                <path d="M9 6V4a1 1 0 0 1 1-1h4a1 1 0 0 1 1 1v2" />
              </svg>
            </button>
          </div>
        </div>
      </li>
    </ul>
  </section>
</template>

<style scoped>
h1 {
  margin-bottom: 1rem;
}

.state {
  padding: 2rem 0;
  text-align: center;
  opacity: 0.8;
}

.state.error {
  color: var(--color-danger);
}

.grid {
  list-style: none;
  padding: 0;
  margin: 0;
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 1.25rem;
}

.tile {
  display: flex;
  flex-direction: column;
}

.thumb {
  display: block;
  aspect-ratio: 1;
  width: 100%;
  border-radius: 8px;
  overflow: hidden;
  background: var(--color-background-mute);
}

.thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.thumb.placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  text-align: center;
  font-size: 0.8rem;
  padding: 0.5rem;
  opacity: 0.6;
}

.tile-info {
  padding: 0.5rem 0.1rem;
}

.title {
  display: block;
  font-size: 0.9rem;
}

.meta {
  font-size: 0.75rem;
  opacity: 0.65;
  margin: 0.15rem 0 0.5rem;
}

.tile-actions {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.review-button {
  background: none;
  border: 1px solid var(--color-border);
  border-radius: 6px;
  padding: 0.25rem 0.6rem;
  font-size: 0.8rem;
  color: var(--color-text);
}

.review-button:hover {
  border-color: var(--color-border-hover);
  text-decoration: none;
}

.delete-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  width: 1.7rem;
  height: 1.7rem;
  border: none;
  border-radius: 6px;
  background: var(--color-danger);
  color: white;
  cursor: pointer;
}

.delete-button:hover:not(:disabled) {
  opacity: 0.85;
}

.delete-button:disabled {
  opacity: 0.5;
  cursor: default;
}
</style>
