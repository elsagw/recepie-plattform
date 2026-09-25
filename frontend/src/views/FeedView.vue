<script setup lang="ts">
import { ref, watch } from 'vue'
import { RouterLink } from 'vue-router'
import { api, ApiError, resolveImageUrl } from '@/api/client'
import { useAuth } from '@/composables/useAuth'
import type { FeedItem, FeedPage } from '@/types/api'
import StarRating from '@/components/StarRating.vue'
import LoadingSpinner from '@/components/LoadingSpinner.vue'
import Skeleton from '@/components/LoadingSkeleton.vue'

const { currentUser, isLoading: authLoading, login } = useAuth()

const steps = [
  {
    title: 'Klistra in en länk',
    text: 'Hitta ett recept du har testat och klistra in länken till det.',
  },
  {
    title: 'Betygsätt och skriv en rad',
    text: 'Ge det 1–5 stjärnor och berätta kort hur det blev.',
  },
  {
    title: 'Se det i feedet',
    text: 'Bläddra bland andras recensioner och spara sånt du vill laga själv.',
  },
]

const previewSamples = [
  {
    title: 'Pannkakor',
    author: 'Maja · igår',
    rating: 5,
    comment: 'Perfekta till söndagsfrukost, körde med havremjölk istället.',
  },
  {
    title: 'Kycklinggryta med kokos',
    author: 'Erik · 3 dagar sedan',
    rating: 4,
    comment: 'Enkel vardagsmat, barnen åt allt utan gnäll.',
  },
  {
    title: 'Pasta carbonara',
    author: 'Sara · 1 vecka sedan',
    rating: 5,
    comment: 'Klassiker som aldrig sviker, blev perfekt krämig.',
  },
]

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

watch(
  () => [authLoading.value, currentUser.value] as const,
  ([loading, user]) => {
    if (!loading && user) {
      loadFeed(0)
    }
  },
  { immediate: true },
)
</script>

<template>
  <section v-if="authLoading" class="auth-check">
    <div class="state"><LoadingSpinner /></div>
  </section>

  <section v-else-if="!currentUser" class="landing">
    <div class="hero">
      <h1>Vad lagade du senast?</h1>
      <p class="lede">
        Dela recensioner av recept du testat, se vad andra lagar och spara sånt du vill laga själv.
      </p>
      <button type="button" class="google-button" @click="login">
        <svg class="google-icon" viewBox="0 0 48 48" width="18" height="18" aria-hidden="true">
          <path
            fill="#FFC107"
            d="M43.611,20.083H42V20H24v8h11.303c-1.649,4.657-6.08,8-11.303,8c-6.627,0-12-5.373-12-12
            c0-6.627,5.373-12,12-12c3.059,0,5.842,1.154,7.961,3.039l5.657-5.657C34.046,6.053,29.268,4,24,4C12.955,4,4,12.955,4,24
            c0,11.045,8.955,20,20,20c11.045,0,20-8.955,20-20C44,22.659,43.862,21.35,43.611,20.083z"
          />
          <path
            fill="#FF3D00"
            d="M6.306,14.691l6.571,4.819C14.655,15.108,18.961,12,24,12c3.059,0,5.842,1.154,7.961,3.039
            l5.657-5.657C34.046,6.053,29.268,4,24,4C16.318,4,9.656,8.337,6.306,14.691z"
          />
          <path
            fill="#4CAF50"
            d="M24,44c5.166,0,9.86-1.977,13.409-5.192l-6.19-5.238C29.211,35.091,26.715,36,24,36
            c-5.202,0-9.619-3.317-11.283-7.946l-6.522,5.025C9.505,39.556,16.227,44,24,44z"
          />
          <path
            fill="#1976D2"
            d="M43.611,20.083H42V20H24v8h11.303c-0.792,2.237-2.231,4.166-4.087,5.571
            c0.001-0.001,0.002-0.001,0.003-0.002l6.19,5.238C36.971,39.205,44,34,44,24
            C44,22.659,43.862,21.35,43.611,20.083z"
          />
        </svg>
        Logga in med Google
      </button>
    </div>

    <div class="how-it-works">
      <h2>Så funkar det</h2>
      <ol class="steps">
        <li v-for="(step, index) in steps" :key="step.title">
          <span class="step-number">{{ index + 1 }}</span>
          <h3>{{ step.title }}</h3>
          <p>{{ step.text }}</p>
        </li>
      </ol>
    </div>

    <div class="preview">
      <h2>Ett smakprov av feedet</h2>
      <ul class="feed-grid">
        <li v-for="sample in previewSamples" :key="sample.title" class="card">
          <div class="photo sample">
            <span class="sample-label">{{ sample.title }}</span>
            <div class="rating-chip">
              <StarRating :model-value="sample.rating" size="sm" />
            </div>
          </div>
          <div class="card-body">
            <h3 class="title">{{ sample.title }}</h3>
            <p class="byline">{{ sample.author }}</p>
            <p class="comment">{{ sample.comment }}</p>
          </div>
        </li>
      </ul>
    </div>
  </section>

  <section v-else class="feed">
    <ul v-if="isLoading" class="feed-grid" aria-hidden="true">
      <li v-for="n in 6" :key="n" class="card">
        <Skeleton class="skeleton-photo" />
        <div class="card-body">
          <Skeleton class="skeleton-title" />
          <Skeleton class="skeleton-byline" />
          <Skeleton class="skeleton-line" />
          <Skeleton class="skeleton-line short" />
        </div>
      </li>
    </ul>

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
          <RouterLink :to="`/recension/${item.reviewId}`" class="photo-link">
            <img v-if="item.imageUrl" :src="resolveImageUrl(item.imageUrl) ?? ''" :alt="item.recipe.title ?? ''" />
            <span v-else class="photo-fallback">{{ item.recipe.domain }}</span>
          </RouterLink>
          <div class="rating-chip">
            <StarRating :model-value="item.rating" size="sm" />
          </div>
          <button
            type="button"
            class="like-button"
            :class="{ saved: item.savedByCurrentUser }"
            :disabled="savePendingFor === item.recipe.id"
            :title="item.savedByCurrentUser ? 'Sparad' : 'Spara till min lista'"
            :aria-label="item.savedByCurrentUser ? 'Sparad, klicka för att ta bort' : 'Spara till min lista'"
            @click="toggleSave(item)"
          >
            <LoadingSpinner v-if="savePendingFor === item.recipe.id" size="sm" />
            <svg v-else viewBox="0 0 24 24" width="15" height="15" fill="currentColor" aria-hidden="true">
              <path
                d="M12 21s-6.716-4.35-9.428-8.03C.688 10.4 1.03 6.9 3.64 5.2 5.94 3.7 8.8 4.3 10.4 6.2L12 8.1l1.6-1.9c1.6-1.9 4.46-2.5 6.76-1 2.61 1.7 2.95 5.2 1.07 7.77C18.72 16.65 12 21 12 21z"
              />
            </svg>
          </button>
        </div>
        <RouterLink :to="`/recension/${item.reviewId}`" class="card-body">
          <h2 class="title">{{ item.recipe.title ?? 'Recept utan titel' }}</h2>
          <div class="byline">
            <span class="byline-avatar">
              <img v-if="item.userAvatarUrl" :src="resolveImageUrl(item.userAvatarUrl) ?? ''" alt="" />
              <svg v-else viewBox="0 0 24 24" width="11" height="11" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <circle cx="12" cy="8" r="4" />
                <path d="M4 20c0-4 3.5-7 8-7s8 3 8 7" />
              </svg>
            </span>
            {{ item.username }} · <time :datetime="item.createdAt">{{ formatDate(item.createdAt) }}</time>
          </div>
          <p v-if="item.comment" class="comment">{{ item.comment }}</p>
        </RouterLink>
      </li>
    </ul>

    <div v-if="!isLoading && !loadError && items.length > 0 && totalElements > size" class="pagination">
      <button v-if="page > 0" type="button" class="btn-outline" @click="loadFeed(page - 1)">
        Föregående
      </button>
      <span class="page-indicator">Sida {{ page + 1 }}</span>
      <button v-if="hasNextPage()" type="button" class="btn-outline" @click="loadFeed(page + 1)">
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

.feed {
  padding-top: 1.5rem;
}

.auth-check {
  padding: 3rem 0;
}

.landing {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 4rem 0 3rem;
}

.hero {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 1.35rem;
  min-height: 80vh;
  max-width: 32rem;
  text-align: center;
}

.hero h1 {
  font-size: clamp(2.25rem, 6vw, 3.25rem);
  line-height: 1.15;
  margin-bottom: 0;
}

.lede {
  font-size: 1.05rem;
  line-height: 1.6;
  color: var(--color-text);
  opacity: 0.75;
  max-width: 34ch;
  margin: 0;
}

.google-button {
  display: inline-flex;
  align-items: center;
  gap: 0.7rem;
  background: var(--color-background);
  border: 1px solid var(--color-border);
  border-radius: 8px;
  padding: 0.75rem 1.5rem;
  font-size: 0.95rem;
  font-weight: 600;
  color: var(--color-heading);
  cursor: pointer;
  margin-top: 0.25rem;
}

.google-button:hover {
  border-color: var(--color-border-hover);
}

.how-it-works,
.preview {
  width: 100%;
  margin-top: 8rem;
}

.how-it-works h2,
.preview h2 {
  font-family: var(--font-display);
  font-weight: 600;
  font-size: 1.4rem;
  text-align: center;
  color: var(--color-heading);
  margin: 0 0 2rem;
}

.steps {
  list-style: none;
  padding: 0;
  margin: 0;
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 2rem;
}

.steps li {
  text-align: center;
}

.step-number {
  display: block;
  font-family: var(--font-display);
  font-weight: 600;
  font-size: 1.75rem;
  color: var(--color-accent);
  margin-bottom: 0.4rem;
}

.steps h3 {
  font-size: 1rem;
  font-weight: 600;
  color: var(--color-heading);
  margin: 0 0 0.4rem;
}

.steps p {
  font-size: 0.88rem;
  line-height: 1.5;
  color: var(--color-text);
  opacity: 0.75;
  max-width: 26ch;
  margin: 0 auto;
}

.photo.sample {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 1rem;
  background: linear-gradient(135deg, var(--color-accent-soft), var(--color-background-mute));
}

.sample-label {
  font-family: var(--font-display);
  font-style: italic;
  font-size: 1.1rem;
  text-align: center;
  color: var(--color-heading);
  opacity: 0.8;
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

.photo-link {
  display: block;
  width: 100%;
  height: 100%;
  color: inherit;
  text-decoration: none;
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

.photo.placeholder .photo-link {
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

.like-button {
  position: absolute;
  right: 0.5rem;
  bottom: 0.5rem;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 1.75rem;
  height: 1.75rem;
  border-radius: 50%;
  border: none;
  background: rgba(0, 0, 0, 0.5);
  color: rgba(255, 255, 255, 0.55);
  cursor: pointer;
}

.like-button.saved {
  color: var(--color-danger);
}

.like-button:disabled {
  opacity: 0.6;
  cursor: default;
}

.card-body,
.card-body:hover,
.photo-link:hover {
  text-decoration: none;
}

.card-body {
  display: block;
  padding-top: 0.85rem;
  color: inherit;
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
  display: flex;
  align-items: center;
  gap: 0.4rem;
  font-size: 0.8rem;
  color: var(--color-text);
  opacity: 0.65;
  margin: 0 0 0.55rem;
}

.byline-avatar {
  flex-shrink: 0;
  width: 1.1rem;
  height: 1.1rem;
  border-radius: 50%;
  overflow: hidden;
  background: var(--color-background-mute);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #9a9a9a;
}

.byline-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
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


.skeleton-photo {
  aspect-ratio: 4 / 3;
  width: 100%;
  border-radius: 4px;
}

.skeleton-title {
  height: 1.05rem;
  width: 70%;
  margin-bottom: 0.5rem;
}

.skeleton-byline {
  height: 0.8rem;
  width: 45%;
  margin-bottom: 0.55rem;
}

.skeleton-line {
  height: 0.85rem;
  width: 100%;
  margin-bottom: 0.4rem;
}

.skeleton-line.short {
  width: 60%;
  margin-bottom: 0;
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
