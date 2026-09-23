<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { api, ApiError, resolveImageUrl } from '@/api/client'
import { useAuth } from '@/composables/useAuth'
import type { CurrentUser, Friend, SavedRecipe } from '@/types/api'
import LoadingSpinner from '@/components/LoadingSpinner.vue'

const { currentUser, setCurrentUser } = useAuth()

const saved = ref<SavedRecipe[]>([])
const friends = ref<Friend[]>([])
const isLoading = ref(true)
const loadError = ref<string | null>(null)

const isEditing = ref(false)
const editName = ref('')
const editEmail = ref('')
const avatarFile = ref<File | null>(null)
const avatarPreviewUrl = ref<string | null>(null)
const isSaving = ref(false)
const saveError = ref<string | null>(null)

async function load() {
  isLoading.value = true
  loadError.value = null
  try {
    const [savedResult, friendsResult] = await Promise.all([
      api.get<SavedRecipe[]>('/api/saved-recipes'),
      api.get<Friend[]>('/api/friends'),
    ])
    saved.value = savedResult
    friends.value = friendsResult
  } catch (error) {
    loadError.value = error instanceof ApiError ? error.message : 'Kunde inte hämta kontot.'
  } finally {
    isLoading.value = false
  }
}

function startEdit() {
  if (!currentUser.value) return
  editName.value = currentUser.value.displayName
  editEmail.value = currentUser.value.email
  saveError.value = null
  isEditing.value = true
}

function cancelEdit() {
  isEditing.value = false
  if (avatarPreviewUrl.value) {
    URL.revokeObjectURL(avatarPreviewUrl.value)
  }
  avatarFile.value = null
  avatarPreviewUrl.value = null
}

function onAvatarSelected(event: Event) {
  const file = (event.target as HTMLInputElement).files?.[0] ?? null
  if (avatarPreviewUrl.value) {
    URL.revokeObjectURL(avatarPreviewUrl.value)
  }
  avatarFile.value = file
  avatarPreviewUrl.value = file ? URL.createObjectURL(file) : null
}

async function saveProfile() {
  isSaving.value = true
  saveError.value = null
  try {
    const updated = await api.put<CurrentUser>('/api/auth/me', {
      displayName: editName.value.trim(),
      email: editEmail.value.trim(),
    })
    setCurrentUser(updated)

    if (avatarFile.value) {
      const formData = new FormData()
      formData.append('image', avatarFile.value)
      const withAvatar = await api.postForm<CurrentUser>('/api/auth/me/avatar', formData)
      setCurrentUser(withAvatar)
    }

    cancelEdit()
  } catch (error) {
    saveError.value = error instanceof ApiError ? error.message : 'Kunde inte spara ändringarna.'
  } finally {
    isSaving.value = false
  }
}

onMounted(load)
</script>

<template>
  <section class="narrow">
    <div v-if="currentUser" class="profile-header">
      <div class="avatar-large">
        <img
          v-if="avatarPreviewUrl || currentUser.avatarUrl"
          :src="avatarPreviewUrl ?? resolveImageUrl(currentUser.avatarUrl) ?? ''"
          alt=""
        />
        <svg
          v-else
          class="avatar-fallback"
          viewBox="0 0 24 24"
          width="42"
          height="42"
          fill="none"
          stroke="currentColor"
          stroke-width="2"
          stroke-linecap="round"
          stroke-linejoin="round"
        >
          <circle cx="12" cy="8" r="4" />
          <path d="M4 20c0-4 3.5-7 8-7s8 3 8 7" />
        </svg>
      </div>
      <div class="identity">
        <h1>{{ currentUser.displayName }}</h1>
        <p class="email">{{ currentUser.email }}</p>
      </div>
      <button
        type="button"
        class="settings-button"
        title="Kontoinställningar"
        aria-label="Kontoinställningar"
        @click="isEditing ? cancelEdit() : startEdit()"
      >
        ⚙️
      </button>
    </div>

    <form v-if="isEditing" class="edit-form" @submit.prevent="saveProfile">
      <div class="field">
        <label for="acc-name">Namn</label>
        <input id="acc-name" v-model="editName" type="text" required maxlength="255" />
      </div>
      <div class="field">
        <label for="acc-email">E-post</label>
        <input id="acc-email" v-model="editEmail" type="email" required maxlength="255" />
      </div>
      <div class="field">
        <label for="acc-avatar">Profilbild</label>
        <input id="acc-avatar" type="file" accept="image/*" @change="onAvatarSelected" />
      </div>
      <p v-if="saveError" class="state error">{{ saveError }}</p>
      <div class="edit-actions">
        <button type="submit" :disabled="isSaving">{{ isSaving ? 'Sparar …' : 'Spara' }}</button>
        <button type="button" class="btn-outline" :disabled="isSaving" @click="cancelEdit">Avbryt</button>
      </div>
    </form>

    <div v-if="isLoading" class="state"><LoadingSpinner /></div>

    <div v-else-if="loadError" class="state error">
      <p>{{ loadError }}</p>
      <button type="button" class="btn-outline" @click="load">Försök igen</button>
    </div>

    <template v-else>
      <div class="section">
        <div class="section-header">
          <h2>Sparat</h2>
          <RouterLink to="/sparat" class="see-all">Se alla ({{ saved.length }})</RouterLink>
        </div>
        <p v-if="saved.length === 0" class="empty">Du har inte sparat några recept än.</p>
        <ul v-else class="saved-strip">
          <li v-for="item in saved.slice(0, 4)" :key="item.recipeId" class="saved-thumb">
            <img v-if="item.imageUrl" :src="resolveImageUrl(item.imageUrl) ?? ''" :alt="item.title ?? ''" />
            <span v-else>{{ item.domain }}</span>
          </li>
        </ul>
      </div>

      <div class="section">
        <div class="section-header">
          <h2>Följer</h2>
          <RouterLink to="/kompisar" class="see-all">Se alla ({{ friends.length }})</RouterLink>
        </div>
        <p v-if="friends.length === 0" class="empty">Du följer inga kompisar än.</p>
        <ul v-else class="friend-list">
          <li v-for="friend in friends.slice(0, 5)" :key="friend.userId" class="friend-chip">
            <span class="friend-avatar">
              <img v-if="friend.avatarUrl" :src="resolveImageUrl(friend.avatarUrl) ?? ''" alt="" />
              <svg
                v-else
                viewBox="0 0 24 24"
                width="19"
                height="19"
                fill="none"
                stroke="currentColor"
                stroke-width="2"
                stroke-linecap="round"
                stroke-linejoin="round"
              >
                <circle cx="12" cy="8" r="4" />
                <path d="M4 20c0-4 3.5-7 8-7s8 3 8 7" />
              </svg>
            </span>
            {{ friend.displayName }}
          </li>
        </ul>
      </div>
    </template>
  </section>
</template>

<style scoped>
.profile-header {
  display: flex;
  align-items: center;
  gap: 1rem;
  margin-bottom: 2rem;
}

.avatar-large {
  flex-shrink: 0;
  width: 4rem;
  height: 4rem;
  border-radius: 50%;
  overflow: hidden;
  background: var(--color-background-mute);
  border: 1px solid var(--color-border);
  display: flex;
  align-items: center;
  justify-content: center;
}

.avatar-large img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.avatar-fallback {
  color: #9a9a9a;
}

.identity {
  flex: 1;
  min-width: 0;
}

h1 {
  font-family: var(--font-display);
  font-weight: 600;
  font-size: 1.4rem;
  color: var(--color-heading);
  margin: 0 0 0.15rem;
}

.email {
  font-size: 0.85rem;
  color: var(--color-text);
  opacity: 0.65;
  margin: 0;
}

.settings-button {
  flex-shrink: 0;
  width: 2.2rem;
  height: 2.2rem;
  border-radius: 50%;
  border: 1px solid var(--color-border);
  background: none;
  font-size: 1.05rem;
  cursor: pointer;
}

.settings-button:hover {
  border-color: var(--color-border-hover);
}

.edit-form {
  display: flex;
  flex-direction: column;
  gap: 1rem;
  padding: 1.25rem;
  margin-bottom: 2rem;
  border: 1px solid var(--color-border);
  border-radius: 10px;
}

.field label {
  display: block;
  font-size: 0.85rem;
  margin-bottom: 0.3rem;
  opacity: 0.8;
}

.field input {
  width: 100%;
  padding: 0.5rem;
  border: 1px solid var(--color-border);
  border-radius: 6px;
  background: var(--color-background-soft);
  color: var(--color-text);
  font: inherit;
}

.edit-actions {
  display: flex;
  gap: 0.75rem;
}

.edit-actions button[type='submit'] {
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

.btn-outline {
  background: none;
  border: 1px solid var(--color-border);
  border-radius: 6px;
  padding: 0.5rem 0.9rem;
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

.state {
  padding: 2rem 0;
  text-align: center;
  opacity: 0.8;
}

.state.error {
  color: var(--color-danger);
  opacity: 1;
}

.section {
  margin-bottom: 2rem;
}

.section-header {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  margin-bottom: 0.85rem;
}

.section-header h2 {
  font-family: var(--font-display);
  font-weight: 600;
  font-size: 1.05rem;
  color: var(--color-heading);
  margin: 0;
}

.see-all {
  font-size: 0.8rem;
}

.empty {
  font-size: 0.85rem;
  color: var(--color-text);
  opacity: 0.6;
  margin: 0;
}

.saved-strip {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  gap: 0.75rem;
}

.saved-thumb {
  width: 4.25rem;
  height: 4.25rem;
  border-radius: 6px;
  overflow: hidden;
  background: var(--color-background-mute);
  border: 1px solid var(--color-border);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.65rem;
  text-align: center;
  color: var(--color-text);
  opacity: 0.7;
}

.saved-thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.friend-list {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 0.6rem;
}

.friend-chip {
  display: flex;
  align-items: center;
  gap: 0.6rem;
  font-size: 0.9rem;
  color: var(--color-text);
}

.friend-avatar {
  flex-shrink: 0;
  width: 1.8rem;
  height: 1.8rem;
  border-radius: 50%;
  overflow: hidden;
  background: var(--color-background-mute);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #9a9a9a;
}

.friend-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}
</style>
