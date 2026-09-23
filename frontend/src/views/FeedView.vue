<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { api, ApiError, resolveImageUrl } from '@/api/client'
import { useAuth } from '@/composables/useAuth'
import type { FeedItem, FeedPage } from '@/types/api'
import StarRating from '@/components/StarRating.vue'

const { currentUser } = useAuth()

const items = ref<FeedItem[]>([])
const page = ref(0)
const size = ref(21)
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

    <ul v-else class="feed-grid">
      <li v-for="item in items" :key="item.reviewId" class="tile">
        <div class="thumb" :class="{ placeholder: !item.imageUrl }">
          <img v-if="item.imageUrl" :src="resolveImageUrl(item.imageUrl) ?? ''" :alt="item.recipe.title ?? ''" />
          <span v-else>{{ item.recipe.domain }}</span>
          <div class="rating-badge">
            <StarRating :model-value="item.rating" size="sm" />
          </div>
          <button
            v-if="currentUser"
            type="button"
            class="save-badge"
            :class="{ saved: item.savedByCurrentUser }"
            :disabled="savePendingFor === item.recipe.id"
            :title="item.savedByCurrentUser ? 'Sparad' : 'Spara till min lista'"
            @click="toggleSave(item)"
          >
            {{ item.savedByCurrentUser ? '✓' : '+' }}
          </button>
        </div>
        <div class="tile-info">
          <strong class="title">{{ item.recipe.title ?? 'Recept utan titel' }}</strong>
          <div class="meta">{{ item.username }} · {{ formatDate(item.createdAt) }}</div>
          <p v-if="item.comment" class="comment">{{ item.comment }}</p>
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

.feed-grid {
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
  position: relative;
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
  color: var(--color-text);
  opacity: 0.6;
}

.rating-badge {
  position: absolute;
  left: 0.4rem;
  bottom: 0.4rem;
  background: rgba(0, 0, 0, 0.55);
  border-radius: 5px;
  padding: 0.15rem 0.4rem;
}

.save-badge {
  position: absolute;
  right: 0.4rem;
  top: 0.4rem;
  width: 1.75rem;
  height: 1.75rem;
  border-radius: 50%;
  border: none;
  background: rgba(0, 0, 0, 0.55);
  color: white;
  font-size: 1rem;
  line-height: 1;
  cursor: pointer;
}

.save-badge.saved {
  background: var(--color-accent);
}

.save-badge:disabled {
  opacity: 0.6;
  cursor: default;
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
  margin: 0.15rem 0;
}

.comment {
  font-size: 0.85rem;
  margin: 0;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 1rem;
  margin-top: 2rem;
}
</style>
