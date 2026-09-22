<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { api, ApiError } from '@/api/client'
import { useAuth } from '@/composables/useAuth'
import type { FeedItem, FeedPage } from '@/types/api'
import StarRating from '@/components/StarRating.vue'

const { currentUser } = useAuth()

const items = ref<FeedItem[]>([])
const page = ref(0)
const size = ref(20)
const totalElements = ref(0)
const isLoading = ref(true)
const loadError = ref<string | null>(null)
const savePendingFor = ref<number | null>(null)

async function loadFeed(targetPage: number) {
  isLoading.value = true
  loadError.value = null
  try {
    const result = await api.get<FeedPage>(`/api/feed?page=${targetPage}&size=${size.value}`)
    items.value = result.content
    page.value = result.page
    totalElements.value = result.totalElements
  } catch (error) {
    loadError.value = error instanceof ApiError ? error.message : 'Kunde inte hämta feedet.'
  } finally {
    isLoading.value = false
  }
}

async function toggleSave(item: FeedItem) {
  if (!currentUser.value) return
  savePendingFor.value = item.recipe.id
  try {
    if (item.savedByCurrentUser) {
      await api.delete(`/api/recipes/${item.recipe.id}/save`)
    } else {
      await api.post(`/api/recipes/${item.recipe.id}/save`)
    }
    for (const feedItem of items.value) {
      if (feedItem.recipe.id === item.recipe.id) {
        feedItem.savedByCurrentUser = !item.savedByCurrentUser
      }
    }
  } catch (error) {
    loadError.value = error instanceof ApiError ? error.message : 'Kunde inte uppdatera sparat recept.'
  } finally {
    savePendingFor.value = null
  }
}

function formatDate(iso: string): string {
  return new Date(iso).toLocaleDateString('sv-SE', { year: 'numeric', month: 'short', day: 'numeric' })
}

const hasNextPage = () => (page.value + 1) * size.value < totalElements.value

onMounted(() => loadFeed(0))
</script>

<template>
  <section>
    <h1>Feed</h1>

    <p v-if="isLoading" class="state">Laddar feed …</p>

    <div v-else-if="loadError" class="state error">
      <p>{{ loadError }}</p>
      <button type="button" @click="loadFeed(page)">Försök igen</button>
    </div>

    <p v-else-if="items.length === 0" class="state">
      Inga recensioner än. <RouterLink to="/add-review">Bli den första att recensera ett recept.</RouterLink>
    </p>

    <ul v-else class="feed-list">
      <li v-for="item in items" :key="item.reviewId" class="card">
        <div class="thumb" :class="{ placeholder: !item.recipe.imageUrl }">
          <img v-if="item.recipe.imageUrl" :src="item.recipe.imageUrl" :alt="item.recipe.title ?? ''" />
          <span v-else>{{ item.recipe.domain }}</span>
        </div>
        <div class="content">
          <div class="title-row">
            <strong>{{ item.recipe.title ?? 'Recept utan titel' }}</strong>
            <span class="domain">{{ item.recipe.domain }}</span>
          </div>
          <StarRating :model-value="item.rating" />
          <p v-if="item.comment" class="comment">{{ item.comment }}</p>
          <div class="meta-row">
            <span>{{ item.username }}</span>
            <span>·</span>
            <span>{{ formatDate(item.createdAt) }}</span>
            <span v-if="item.updatedAt !== item.createdAt">(redigerad)</span>
          </div>
          <button
            v-if="currentUser"
            type="button"
            class="save-button"
            :class="{ saved: item.savedByCurrentUser }"
            :disabled="savePendingFor === item.recipe.id"
            @click="toggleSave(item)"
          >
            {{ item.savedByCurrentUser ? '✓ Sparad' : '+ Spara till min lista' }}
          </button>
        </div>
      </li>
    </ul>

    <div v-if="!isLoading && !loadError && items.length > 0" class="pagination">
      <button type="button" :disabled="page === 0" @click="loadFeed(page - 1)">Föregående</button>
      <span>Sida {{ page + 1 }}</span>
      <button type="button" :disabled="!hasNextPage()" @click="loadFeed(page + 1)">Nästa</button>
    </div>
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

.feed-list {
  list-style: none;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

.card {
  display: flex;
  gap: 1rem;
  border: 1px solid var(--color-border);
  border-radius: 8px;
  padding: 1rem;
}

.thumb {
  flex-shrink: 0;
  width: 96px;
  height: 96px;
  border-radius: 6px;
  overflow: hidden;
  background: var(--color-background-mute);
}

.thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.thumb.placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  text-align: center;
  font-size: 0.75rem;
  padding: 0.25rem;
  color: var(--color-text);
  opacity: 0.6;
}

.content {
  flex: 1;
  min-width: 0;
}

.title-row {
  display: flex;
  justify-content: space-between;
  gap: 0.5rem;
  margin-bottom: 0.25rem;
}

.domain {
  font-size: 0.8rem;
  opacity: 0.6;
  white-space: nowrap;
}

.comment {
  margin: 0.4rem 0;
}

.meta-row {
  display: flex;
  gap: 0.4rem;
  font-size: 0.8rem;
  opacity: 0.7;
}

.save-button {
  margin-top: 0.5rem;
  background: none;
  border: 1px solid var(--color-accent);
  color: var(--color-accent);
  border-radius: 6px;
  padding: 0.3rem 0.7rem;
  font-size: 0.85rem;
  cursor: pointer;
}

.save-button.saved {
  background: var(--color-accent-soft);
}

.save-button:disabled {
  opacity: 0.6;
  cursor: default;
}

.pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 1rem;
  margin-top: 1.5rem;
}
</style>
