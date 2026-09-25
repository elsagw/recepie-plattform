<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { api, ApiError, resolveImageUrl } from '@/api/client'
import { useAuth } from '@/composables/useAuth'
import type { ExternalRecipe, MyReview, Review } from '@/types/api'
import StarRating from '@/components/StarRating.vue'
import LoadingSpinner from '@/components/LoadingSpinner.vue'

const router = useRouter()
const route = useRoute()
const { currentUser, login } = useAuth()

const MAX_COMMENT_LENGTH = 2000

const url = ref('')
const recipe = ref<ExternalRecipe | null>(null)
const myReviews = ref<MyReview[]>([])
const rating = ref(0)
const comment = ref('')
const imageFile = ref<File | null>(null)
const imagePreviewUrl = ref<string | null>(null)

const isScraping = ref(false)
const scrapeError = ref<string | null>(null)
const isSubmitting = ref(false)
const submitError = ref<string | null>(null)

function onImageSelected(event: Event) {
  const file = (event.target as HTMLInputElement).files?.[0] ?? null
  if (imagePreviewUrl.value) {
    URL.revokeObjectURL(imagePreviewUrl.value)
  }
  imageFile.value = file
  imagePreviewUrl.value = file ? URL.createObjectURL(file) : null
}

async function scrapeUrl() {
  if (!url.value.trim()) return
  isScraping.value = true
  scrapeError.value = null
  recipe.value = null
  myReviews.value = []
  try {
    recipe.value = await api.post<ExternalRecipe>(
      `/api/recipes/scrape?url=${encodeURIComponent(url.value.trim())}`,
    )
    await loadMyReviews(recipe.value.id)
  } catch (error) {
    scrapeError.value = error instanceof ApiError ? error.message : 'Kunde inte förhandsgranska länken.'
  } finally {
    isScraping.value = false
  }
}

async function loadMyReviews(recipeId: number) {
  try {
    myReviews.value = await api.get<MyReview[]>(`/api/recipes/${recipeId}/reviews/mine`)
  } catch {
    // Non-critical - the add-review flow still works without the history list.
    myReviews.value = []
  }
}

async function publish() {
  if (!recipe.value || rating.value < 1) return
  isSubmitting.value = true
  submitError.value = null
  try {
    const created = await api.post<Review>('/api/reviews', {
      recipeId: recipe.value.id,
      rating: rating.value,
      comment: comment.value.trim() || null,
    })

    if (imageFile.value) {
      const formData = new FormData()
      formData.append('image', imageFile.value)
      await api.postForm<Review>(`/api/reviews/${created.reviewId}/image`, formData)
    }

    router.push('/feed')
  } catch (error) {
    submitError.value = error instanceof ApiError ? error.message : 'Kunde inte publicera recensionen.'
  } finally {
    isSubmitting.value = false
  }
}

onMounted(() => {
  const queryUrl = route.query.url
  if (typeof queryUrl === 'string' && queryUrl) {
    url.value = queryUrl
    if (currentUser.value) {
      scrapeUrl()
    }
  }
})
</script>

<template>
  <section>
    <h1>Recensera ett recept</h1>

    <div v-if="!currentUser" class="state">
      <p>Du måste vara inloggad för att förhandsgranska och recensera recept.</p>
      <button type="button" class="login-button" @click="login">Logga in med Google</button>
    </div>

    <template v-else>
      <form class="url-form" @submit.prevent="scrapeUrl">
        <label for="url">Länk till receptet</label>
        <div class="url-row">
          <input
            id="url"
            v-model="url"
            type="url"
            placeholder="https://example.com/recept/..."
            required
          />
          <button type="submit" :disabled="isScraping">
            <LoadingSpinner v-if="isScraping" size="sm" />
            {{ isScraping ? 'Hämtar …' : 'Förhandsgranska' }}
          </button>
        </div>
      </form>

      <p v-if="scrapeError" class="state error">{{ scrapeError }}</p>

      <div v-if="recipe" class="preview">
        <div class="thumb" :class="{ placeholder: !recipe.imageUrl }">
          <img v-if="recipe.imageUrl" :src="resolveImageUrl(recipe.imageUrl) ?? ''" :alt="recipe.title ?? ''" />
          <span v-else>{{ recipe.domain }}</span>
        </div>
        <div>
          <strong>{{ recipe.title ?? 'Recept utan titel' }}</strong>
          <div class="domain">{{ recipe.domain }}</div>
        </div>
      </div>

      <div v-if="recipe && myReviews.length > 0" class="my-reviews">
        <h2>Dina tidigare recensioner av det här receptet</h2>
        <ul>
          <li v-for="review in myReviews" :key="review.reviewId">
            <StarRating :model-value="review.rating" />
            <span v-if="review.comment">{{ review.comment }}</span>
          </li>
        </ul>
      </div>

      <form v-if="recipe" class="review-form" @submit.prevent="publish">
        <div class="field">
          <span class="field-label">Betyg</span>
          <StarRating v-model="rating" interactive />
        </div>
        <div class="field">
          <label for="comment">Kommentar (valfritt)</label>
          <textarea id="comment" v-model="comment" rows="4" :maxlength="MAX_COMMENT_LENGTH"></textarea>
          <p class="char-count" :class="{ limit: comment.length >= MAX_COMMENT_LENGTH }">
            {{ comment.length }} / {{ MAX_COMMENT_LENGTH }} tecken
          </p>
        </div>
        <div class="field">
          <label for="image">Egen bild på receptet (valfritt)</label>
          <input id="image" type="file" accept="image/*" @change="onImageSelected" />
          <p class="hint">Har du ingen egen bild används receptets bild istället.</p>
          <img v-if="imagePreviewUrl" :src="imagePreviewUrl" alt="" class="image-preview" />
        </div>
        <p v-if="submitError" class="state error">{{ submitError }}</p>
        <button type="submit" :disabled="rating < 1 || isSubmitting">
          <LoadingSpinner v-if="isSubmitting" size="sm" />
          {{ isSubmitting ? 'Publicerar …' : 'Publicera recension' }}
        </button>
      </form>
    </template>
  </section>
</template>

<style scoped>
h1 {
  margin-bottom: 1rem;
}

.state {
  padding: 1.5rem 0;
}

.state.error {
  color: var(--color-danger);
}

.login-button {
  background: var(--color-accent);
  color: white;
  border: none;
  border-radius: 6px;
  padding: 0.5rem 0.9rem;
  cursor: pointer;
}

.url-form label,
.field label,
.field-label {
  display: block;
  font-size: 0.85rem;
  margin-bottom: 0.3rem;
  opacity: 0.8;
}

.url-row {
  display: flex;
  gap: 0.5rem;
}

.url-row input,
.review-form textarea {
  background: var(--color-background-soft);
  color: var(--color-text);
}

.url-row input {
  flex: 1;
  padding: 0.5rem;
  border: 1px solid var(--color-border);
  border-radius: 6px;
}

.url-row button,
.review-form button {
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
  background: var(--color-accent);
  color: white;
  border: none;
  border-radius: 6px;
  padding: 0.5rem 0.9rem;
  cursor: pointer;
}

.url-row button:disabled,
.review-form button:disabled {
  opacity: 0.6;
  cursor: default;
}

.preview {
  display: flex;
  gap: 1rem;
  align-items: center;
  margin-top: 1.25rem;
  padding: 1rem;
  border: 1px solid var(--color-border);
  border-radius: 8px;
}

.thumb {
  flex-shrink: 0;
  width: 72px;
  height: 72px;
  border-radius: 6px;
  overflow: hidden;
  background: var(--color-background-mute);
  display: flex;
  align-items: center;
  justify-content: center;
}

.thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.thumb.placeholder {
  font-size: 0.7rem;
  text-align: center;
  padding: 0.25rem;
  opacity: 0.6;
}

.domain {
  font-size: 0.8rem;
  opacity: 0.6;
}

.my-reviews {
  margin-top: 1.25rem;
}

.my-reviews h2 {
  font-size: 0.95rem;
  margin-bottom: 0.5rem;
}

.my-reviews ul {
  list-style: none;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 0.4rem;
}

.my-reviews li {
  display: flex;
  align-items: center;
  gap: 0.6rem;
  font-size: 0.9rem;
}

.review-form {
  margin-top: 1.5rem;
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.review-form textarea {
  width: 100%;
  padding: 0.5rem;
  border: 1px solid var(--color-border);
  border-radius: 6px;
  font: inherit;
  resize: vertical;
}

.hint {
  font-size: 0.8rem;
  opacity: 0.6;
  margin: 0.3rem 0 0;
}

.char-count {
  font-size: 0.78rem;
  color: var(--color-text);
  opacity: 0.55;
  text-align: right;
  margin: 0.3rem 0 0;
}

.char-count.limit {
  color: var(--color-danger);
  opacity: 1;
}

.image-preview {
  margin-top: 0.6rem;
  max-width: 200px;
  max-height: 200px;
  border-radius: 8px;
  object-fit: cover;
  display: block;
}
</style>
