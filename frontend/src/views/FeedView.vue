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
      <button type="button" class="btn-outline" @click="loadFeed(page)">Försök igen</button>
    </div>

    <p v-else-if="items.length === 0" class="state">
      Inga recensioner än. <RouterLink to="/add-review">Bli den första att recensera ett recept.</RouterLink>
    </p>

    <ul v-else class="feed-grid">
      <li v-for="item in items" :key="item.reviewId" class="card">
        <div class="photo" :class="{ placeholder: !item.imageUrl }">
          <img v-if="item.imageUrl" :src="resolveImageUrl(item.imageUrl) ?? ''" :alt="item.recipe.title ?? ''" />
          <span v-else class="photo-fallback">{{ item.recipe.domain }}</span>
          <div class="rating-chip">
            <StarRating :model-value="item.rating" size="sm" />
          </div>
          <button
            v-if="currentUser"
            type="button"
            class="save-button"
            :class="{ saved: item.savedByCurrentUser }"
            :disabled="savePendingFor === item.recipe.id"
            :title="item.savedByCurrentUser ? 'Sparad' : 'Spara till min lista'"
            @click="toggleSave(item)"
          >
            {{ item.savedByCurrentUser ? '✓' : '+' }}
          </button>
        </div>
        <div class="card-body">
          <h2 class="title">{{ item.recipe.title ?? 'Recept utan titel' }}</h2>
          <p class="byline">{{ item.username }} · <time :datetime="item.createdAt">{{ formatDate(item.createdAt) }}</time></p>
          <p v-if="item.comment" class="comment">{{ item.comment }}</p>
        </div>
      </li>
    </ul>

    <div v-if="!isLoading && !loadError && items.length > 0" class="pagination">
      <button type="button" class="btn-outline" :disabled="page === 0" @click="loadFeed(page - 1)">
        Föregående
      </button>
      <span class="page-indicator">Sida {{ page + 1 }}</span>
      <button type="button" class="btn-outline" :disabled="!hasNextPage()" @click="loadFeed(page + 1)">
        Nästa
      </button>
    </div>
  </section>
</template>

<style scoped>
h1 {
  font-family: var(--font-display);
  font-weight: 600;
  font-size: clamp(1.75rem, 4vw, 2.25rem);
  letter-spacing: -0.01em;
  color: var(--color-heading);
  margin-bottom: 2rem;
}

.state {
  padding: 3rem 0;
  text-align: center;
  opacity: 0.8;
}

.state.error {
  color: var(--color-danger);
  opacity: 1;
}

.state.error button {
  margin-top: 0.85rem;
}

.btn-outline {
  background: none;
  border: 1px solid var(--color-border);
  border-radius: 6px;
  padding: 0.45rem 0.9rem;
  color: var(--color-text);
  cursor: pointer;
}

.btn-outline:hover:not(:disabled) {
  border-color: var(--color-border-hover);
}

.btn-outline:disabled {
  opacity: 0.4;
  cursor: default;
}

.feed-grid {
  list-style: none;
  padding: 0;
  margin: 0;
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 2rem 1.75rem;
}

.card {
  display: flex;
  flex-direction: column;
}

.photo {
  position: relative;
  aspect-ratio: 4 / 3;
  width: 100%;
  border-radius: 4px;
  overflow: hidden;
  background: var(--color-background-mute);
  border: 1px solid var(--color-border);
}

.photo img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

@media (prefers-reduced-motion: no-preference) {
  .photo img {
    transition: transform 0.35s ease;
  }

  .card:hover .photo img {
    transform: scale(1.035);
  }
}

.photo.placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
}

.photo-fallback {
  text-align: center;
  font-size: 0.8rem;
  padding: 0.5rem;
  color: var(--color-text);
  opacity: 0.6;
}

.rating-chip {
  position: absolute;
  left: 0.5rem;
  bottom: 0.5rem;
  background: rgba(0, 0, 0, 0.55);
  border-radius: 999px;
  padding: 0.2rem 0.5rem;
  line-height: 1;
}

.save-button {
  position: absolute;
  right: 0.5rem;
  top: 0.5rem;
  width: 1.75rem;
  height: 1.75rem;
  border-radius: 50%;
  border: none;
  background: rgba(0, 0, 0, 0.5);
  color: white;
  font-size: 1rem;
  line-height: 1;
  cursor: pointer;
}

.save-button.saved {
  background: var(--color-accent);
}

.save-button:disabled {
  opacity: 0.6;
  cursor: default;
}

.card-body {
  padding-top: 0.85rem;
}

.title {
  font-family: var(--font-display);
  font-weight: 600;
  font-size: 1.05rem;
  line-height: 1.35;
  color: var(--color-heading);
  margin: 0 0 0.3rem;
}

.byline {
  font-size: 0.8rem;
  color: var(--color-text);
  opacity: 0.65;
  margin: 0 0 0.55rem;
}

.comment {
  font-size: 0.92rem;
  font-style: italic;
  line-height: 1.5;
  color: var(--color-text);
  margin: 0;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 1.25rem;
  margin-top: 3rem;
}

.page-indicator {
  font-size: 0.85rem;
  opacity: 0.65;
}
</style>
