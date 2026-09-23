<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { api, ApiError } from '@/api/client'
import type { Friend } from '@/types/api'
import LoadingSpinner from '@/components/LoadingSpinner.vue'

const friends = ref<Friend[]>([])
const isLoading = ref(true)
const loadError = ref<string | null>(null)

const email = ref('')
const isAdding = ref(false)
const addError = ref<string | null>(null)
const addSuccess = ref<string | null>(null)
const removePendingFor = ref<number | null>(null)

async function load() {
  isLoading.value = true
  loadError.value = null
  try {
    friends.value = await api.get<Friend[]>('/api/friends')
  } catch (error) {
    loadError.value = error instanceof ApiError ? error.message : 'Kunde inte hämta kompislistan.'
  } finally {
    isLoading.value = false
  }
}

async function addFriend() {
  if (!email.value.trim()) return
  isAdding.value = true
  addError.value = null
  addSuccess.value = null
  try {
    const friend = await api.post<Friend>('/api/friends', { email: email.value.trim() })
    if (!friends.value.some((f) => f.userId === friend.userId)) {
      friends.value.unshift(friend)
    }
    addSuccess.value = `${friend.displayName} tillagd som kompis!`
    email.value = ''
  } catch (error) {
    addError.value = error instanceof ApiError ? error.message : 'Kunde inte lägga till kompisen.'
  } finally {
    isAdding.value = false
  }
}

async function removeFriend(userId: number) {
  removePendingFor.value = userId
  try {
    await api.delete(`/api/friends/${userId}`)
    friends.value = friends.value.filter((f) => f.userId !== userId)
  } catch (error) {
    loadError.value = error instanceof ApiError ? error.message : 'Kunde inte ta bort kompisen.'
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
  <section class="narrow">
    <h1>Kompisar</h1>

    <form class="add-form" @submit.prevent="addFriend">
      <label for="friend-email">Lägg till kompis (e-post)</label>
      <div class="row">
        <input id="friend-email" v-model="email" type="email" placeholder="namn@example.com" required />
        <button type="submit" :disabled="isAdding">{{ isAdding ? 'Lägger till …' : 'Lägg till' }}</button>
      </div>
      <p v-if="addError" class="state error">{{ addError }}</p>
      <p v-if="addSuccess" class="state success">{{ addSuccess }}</p>
    </form>

    <div v-if="isLoading" class="state loading"><LoadingSpinner /></div>

    <div v-else-if="loadError" class="state error">
      <p>{{ loadError }}</p>
      <button type="button" @click="load">Försök igen</button>
    </div>

    <p v-else-if="friends.length === 0" class="state">Du har inte lagt till några kompisar än.</p>

    <ul v-else class="friend-list">
      <li v-for="friend in friends" :key="friend.userId">
        <div>
          <strong>{{ friend.displayName }}</strong>
          <div class="meta">Tillagd {{ formatDate(friend.addedAt) }}</div>
        </div>
        <button
          type="button"
          class="remove-button"
          :disabled="removePendingFor === friend.userId"
          @click="removeFriend(friend.userId)"
        >
          Ta bort
        </button>
      </li>
    </ul>
  </section>
</template>

<style scoped>
h1 {
  margin-bottom: 1rem;
}

.add-form {
  margin-bottom: 1.5rem;
}

.add-form label {
  display: block;
  font-size: 0.85rem;
  margin-bottom: 0.3rem;
  opacity: 0.8;
}

.row {
  display: flex;
  gap: 0.5rem;
}

.row input {
  flex: 1;
  padding: 0.5rem;
  border: 1px solid var(--color-border);
  border-radius: 6px;
  background: var(--color-background-soft);
  color: var(--color-text);
}

.row button {
  background: var(--color-accent);
  color: white;
  border: none;
  border-radius: 6px;
  padding: 0.5rem 0.9rem;
  cursor: pointer;
}

.row button:disabled {
  opacity: 0.6;
  cursor: default;
}

.state {
  padding: 1rem 0;
}

.state.loading {
  text-align: center;
}

.state.error {
  color: var(--color-danger);
}

.state.success {
  color: var(--color-secondary);
}

.friend-list {
  list-style: none;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.friend-list li {
  display: flex;
  justify-content: space-between;
  align-items: center;
  border: 1px solid var(--color-border);
  border-radius: 8px;
  padding: 0.75rem 1rem;
}

.meta {
  font-size: 0.75rem;
  opacity: 0.65;
  margin-top: 0.15rem;
}

.remove-button {
  background: none;
  border: 1px solid var(--color-border);
  border-radius: 6px;
  padding: 0.35rem 0.7rem;
  font-size: 0.8rem;
  cursor: pointer;
}

.remove-button:disabled {
  opacity: 0.6;
  cursor: default;
}
</style>
