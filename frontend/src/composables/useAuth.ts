import { ref } from 'vue'
import { API_BASE_URL, ApiError, api } from '@/api/client'
import type { CurrentUser } from '@/types/api'

// Module-level state so every component sharing this composable sees the same user -
// small enough app that a Pinia store would be pure ceremony.
const currentUser = ref<CurrentUser | null>(null)
const isLoading = ref(true)
let initialized = false

async function fetchCurrentUser() {
  isLoading.value = true
  try {
    currentUser.value = await api.get<CurrentUser>('/api/auth/me')
  } catch (error) {
    if (!(error instanceof ApiError && error.status === 401)) {
      console.error('Kunde inte hämta inloggad användare', error)
    }
    currentUser.value = null
  } finally {
    isLoading.value = false
  }
}

function login() {
  // Full-page navigation, not a fetch - this is Spring Security's redirect-based OAuth2 flow.
  window.location.href = `${API_BASE_URL}/oauth2/authorization/google`
}

async function logout() {
  await api.post('/api/auth/logout')
  currentUser.value = null
}

export function useAuth() {
  if (!initialized) {
    initialized = true
    void fetchCurrentUser()
  }
  return { currentUser, isLoading, login, logout, refresh: fetchCurrentUser }
}
