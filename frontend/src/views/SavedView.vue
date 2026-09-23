<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { api, ApiError, resolveImageUrl } from '@/api/client'
import type { SavedRecipe } from '@/types/api'

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

    <p v-if="isLoading" class="state">Laddar …</p>

    <div v-else-if="loadError" class="state error">
      <p>{{ loadError }}</p>
      <button type="button" @click="load">Försök igen</button>
    </div>

    <p v-else-if="items.length === 0" class="state">
      Du har inte sparat några recept än. <RouterLink to="/feed">Bläddra i feedet</RouterLink> och spara något.
    </p>

    <ul v-else class="grid">
      <li v-for="item in items" :key="item.recipeId" class="tile">
        <div class="thumb" :class="{ placeholder: !item.imageUrl }">
          <img v-if="item.imageUrl" :src="resolveImageUrl(item.imageUrl) ?? ''" :alt="item.title ?? ''" />
          <span v-else>{{ item.domain }}</span>
        </div>
        <div class="tile-info">
          <strong class="title">{{ item.title ?? 'Recept utan titel' }}</strong>
          <div class="meta">{{ item.domain }} · sparad {{ formatDate(item.savedAt) }}</div>
          <button
            type="button"
            class="remove-button"
            :disabled="removePendingFor === item.recipeId"
            @click="remove(item.recipeId)"
          >
            Ta bort
          </button>
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

.remove-button {
  background: none;
  border: 1px solid var(--color-border);
  border-radius: 6px;
  padding: 0.25rem 0.6rem;
  font-size: 0.8rem;
  cursor: pointer;
}

.remove-button:disabled {
  opacity: 0.6;
  cursor: default;
}
</style>
