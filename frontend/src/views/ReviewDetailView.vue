<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter, RouterLink } from 'vue-router'
import { api, ApiError, resolveImageUrl } from '@/api/client'
import type { Review, ReviewDetail } from '@/types/api'
import StarRating from '@/components/StarRating.vue'
import LoadingSpinner from '@/components/LoadingSpinner.vue'
import Skeleton from '@/components/LoadingSkeleton.vue'

const route = useRoute()
const router = useRouter()
const reviewId = Number(route.params.id)

const MAX_COMMENT_LENGTH = 2000

const review = ref<ReviewDetail | null>(null)
const isLoading = ref(true)
const loadError = ref<string | null>(null)

const isEditing = ref(false)
const editRating = ref(0)
const editComment = ref('')
const isSaving = ref(false)
const saveError = ref<string | null>(null)

const isConfirmingDelete = ref(false)
const isDeleting = ref(false)
const deleteError = ref<string | null>(null)

async function load() {
  isLoading.value = true
  loadError.value = null
  try {
    review.value = await api.get<ReviewDetail>(`/api/reviews/${reviewId}`)
  } catch (error) {
    loadError.value = error instanceof ApiError ? error.message : 'Kunde inte hämta recensionen.'
  } finally {
    isLoading.value = false
  }
}

function formatDate(iso: string): string {
  return new Date(iso).toLocaleDateString('sv-SE', { year: 'numeric', month: 'short', day: 'numeric' })
}

function startEdit() {
  if (!review.value) return
  editRating.value = review.value.rating
  editComment.value = review.value.comment ?? ''
  saveError.value = null
  isEditing.value = true
}

function cancelEdit() {
  isEditing.value = false
}

async function saveEdit() {
  if (!review.value || editRating.value < 1) return
  isSaving.value = true
  saveError.value = null
  try {
    const updated = await api.put<Review>(`/api/reviews/${review.value.reviewId}`, {
      rating: editRating.value,
      comment: editComment.value.trim() || null,
    })
    review.value = {
      ...review.value,
      rating: updated.rating,
      comment: updated.comment,
      updatedAt: updated.updatedAt,
    }
    isEditing.value = false
  } catch (error) {
    saveError.value = error instanceof ApiError ? error.message : 'Kunde inte spara ändringarna.'
  } finally {
    isSaving.value = false
  }
}

async function deleteReview() {
  if (!review.value) return
  isDeleting.value = true
  deleteError.value = null
  try {
    await api.delete(`/api/reviews/${review.value.reviewId}`)
    router.push('/feed')
  } catch (error) {
    deleteError.value = error instanceof ApiError ? error.message : 'Kunde inte ta bort recensionen.'
    isDeleting.value = false
  }
}

onMounted(load)
</script>

<template>
  <section class="narrow">
    <RouterLink to="/feed" class="back-link" title="Till feedet" aria-label="Till feedet">
      <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
        <path d="M19 12H5" />
        <path d="M11 18l-6-6 6-6" />
      </svg>
    </RouterLink>

    <div v-if="isLoading" aria-hidden="true">
      <Skeleton class="photo" />
      <Skeleton class="skeleton-heading" />
      <Skeleton class="skeleton-domain" />
      <div class="meta-row">
        <Skeleton class="skeleton-rating" />
        <Skeleton class="skeleton-byline" />
      </div>
      <Skeleton class="skeleton-comment" />
    </div>

    <div v-else-if="loadError" class="state error">
      <p>{{ loadError }}</p>
      <button type="button" class="btn-outline" @click="load">Försök igen</button>
    </div>

    <template v-else-if="review">
      <div class="photo" :class="{ placeholder: !review.imageUrl }">
        <img v-if="review.imageUrl" :src="resolveImageUrl(review.imageUrl) ?? ''" :alt="review.recipe.title ?? ''" />
        <span v-else>{{ review.recipe.domain }}</span>
      </div>

      <h1>{{ review.recipe.title ?? 'Recept utan titel' }}</h1>
      <p class="domain">{{ review.recipe.domain }}</p>

      <div class="meta-row">
        <StarRating :model-value="review.rating" />
        <p class="byline">
          <span class="byline-avatar">
            <img v-if="review.userAvatarUrl" :src="resolveImageUrl(review.userAvatarUrl) ?? ''" alt="" />
            <svg v-else viewBox="0 0 24 24" width="12" height="12" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <circle cx="12" cy="8" r="4" />
              <path d="M4 20c0-4 3.5-7 8-7s8 3 8 7" />
            </svg>
          </span>
          {{ review.username }} · <time :datetime="review.createdAt">{{ formatDate(review.createdAt) }}</time>
          <span v-if="review.updatedAt !== review.createdAt"> · redigerad</span>
        </p>
      </div>

      <p v-if="review.comment && !isEditing" class="comment">{{ review.comment }}</p>

      <div v-if="review.ownedByCurrentUser && !isEditing" class="owner-actions">
        <button type="button" class="btn-outline" @click="startEdit">Redigera</button>
        <button
          v-if="!isConfirmingDelete"
          type="button"
          class="delete-button"
          title="Ta bort recension"
          aria-label="Ta bort recension"
          @click="isConfirmingDelete = true"
        >
          <svg viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <polyline points="3 6 5 6 21 6"></polyline>
            <path d="M19 6l-1 14a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2L5 6" />
            <path d="M10 11v6" />
            <path d="M14 11v6" />
            <path d="M9 6V4a1 1 0 0 1 1-1h4a1 1 0 0 1 1 1v2" />
          </svg>
        </button>
        <template v-else>
          <span class="confirm-text">Ta bort recensionen?</span>
          <button type="button" class="confirm-delete" :disabled="isDeleting" @click="deleteReview">
            <LoadingSpinner v-if="isDeleting" size="sm" />
            {{ isDeleting ? 'Tar bort …' : 'Ja, ta bort' }}
          </button>
          <button type="button" class="btn-outline" :disabled="isDeleting" @click="isConfirmingDelete = false">
            Avbryt
          </button>
        </template>
      </div>
      <p v-if="deleteError" class="state error">{{ deleteError }}</p>

      <form v-if="isEditing" class="edit-form" @submit.prevent="saveEdit">
        <div class="field">
          <span class="field-label">Betyg</span>
          <StarRating v-model="editRating" interactive />
        </div>
        <div class="field">
          <label for="edit-comment">Kommentar (valfritt)</label>
          <textarea id="edit-comment" v-model="editComment" rows="4" :maxlength="MAX_COMMENT_LENGTH"></textarea>
          <p class="char-count" :class="{ limit: editComment.length >= MAX_COMMENT_LENGTH }">
            {{ editComment.length }} / {{ MAX_COMMENT_LENGTH }} tecken
          </p>
        </div>
        <p v-if="saveError" class="state error">{{ saveError }}</p>
        <div class="edit-actions">
          <button type="submit" :disabled="editRating < 1 || isSaving">
            <LoadingSpinner v-if="isSaving" size="sm" />
            {{ isSaving ? 'Sparar …' : 'Spara ändringar' }}
          </button>
          <button type="button" class="btn-outline" :disabled="isSaving" @click="cancelEdit">Avbryt</button>
        </div>
      </form>
    </template>
  </section>
</template>

<style scoped>
.back-link {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 2rem;
  height: 2rem;
  border-radius: 50%;
  color: var(--color-text);
  text-decoration: none;
  margin-bottom: 1.5rem;
}

.back-link:hover {
  background: var(--color-background-mute);
  text-decoration: none;
}

.state {
  padding: 2rem 0;
  text-align: center;
  opacity: 0.8;
}

.state.error {
  color: var(--color-danger);
  opacity: 1;
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

.photo {
  aspect-ratio: 4 / 3;
  width: 100%;
  border-radius: 6px;
  overflow: hidden;
  background: var(--color-background-mute);
  border: 1px solid var(--color-border);
  margin-bottom: 1.25rem;
}

.photo img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.photo.placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-text);
  opacity: 0.6;
  font-size: 0.85rem;
}

.skeleton-heading {
  height: 1.75rem;
  width: 70%;
  margin-bottom: 0.5rem;
}

.skeleton-domain {
  height: 0.85rem;
  width: 30%;
  margin-bottom: 1.25rem;
}

.skeleton-rating {
  height: 1.1rem;
  width: 6rem;
}

.skeleton-byline {
  height: 0.85rem;
  width: 8rem;
}

.skeleton-comment {
  height: 3rem;
  width: 100%;
  margin-top: 0.5rem;
}

h1 {
  font-family: var(--font-display);
  font-weight: 600;
  font-size: 1.75rem;
  line-height: 1.25;
  color: var(--color-heading);
  margin: 0 0 0.2rem;
}

.domain {
  font-size: 0.85rem;
  color: var(--color-text);
  opacity: 0.6;
  margin: 0 0 1.25rem;
}

.meta-row {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  flex-wrap: wrap;
  margin-bottom: 1.25rem;
}

.byline {
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
  font-size: 0.85rem;
  color: var(--color-text);
  opacity: 0.7;
  margin: 0;
}

.byline-avatar {
  flex-shrink: 0;
  width: 1.3rem;
  height: 1.3rem;
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
  font-size: 1rem;
  font-style: italic;
  line-height: 1.65;
  color: var(--color-text);
  margin: 0 0 2rem;
  white-space: pre-wrap;
}

.owner-actions {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  flex-wrap: wrap;
  margin-bottom: 2rem;
}

.delete-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 1.9rem;
  height: 1.9rem;
  border: none;
  border-radius: 6px;
  background: var(--color-danger);
  color: white;
  cursor: pointer;
}

.delete-button:hover {
  opacity: 0.85;
}

.confirm-text {
  font-size: 0.85rem;
  color: var(--color-text);
  opacity: 0.8;
}

.confirm-delete {
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
  background: var(--color-danger);
  color: white;
  border: none;
  border-radius: 6px;
  padding: 0.45rem 0.9rem;
  cursor: pointer;
}

.confirm-delete:disabled {
  opacity: 0.6;
  cursor: default;
}

.edit-form {
  display: flex;
  flex-direction: column;
  gap: 1rem;
  margin-bottom: 2rem;
}

.field label,
.field-label {
  display: block;
  font-size: 0.85rem;
  margin-bottom: 0.3rem;
  opacity: 0.8;
}

.edit-form textarea {
  width: 100%;
  padding: 0.5rem;
  border: 1px solid var(--color-border);
  border-radius: 6px;
  background: var(--color-background-soft);
  color: var(--color-text);
  font: inherit;
  resize: vertical;
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

.edit-actions {
  display: flex;
  gap: 0.75rem;
}

.edit-actions button[type='submit'] {
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

.edit-actions button[type='submit']:disabled {
  opacity: 0.6;
  cursor: default;
}
</style>
