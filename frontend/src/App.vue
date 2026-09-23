<script setup lang="ts">
import { RouterLink, RouterView } from 'vue-router'
import { useAuth } from '@/composables/useAuth'

const { currentUser, isLoading, login, logout } = useAuth()
</script>

<template>
  <header>
    <div class="bar">
      <RouterLink to="/feed" class="brand">receptfeed</RouterLink>
      <nav>
        <RouterLink to="/feed">Feed</RouterLink>
        <RouterLink v-if="currentUser" to="/add-review">Recensera</RouterLink>
        <RouterLink v-if="currentUser" to="/sparat">Sparat</RouterLink>
        <RouterLink v-if="currentUser" to="/kompisar">Kompisar</RouterLink>
      </nav>
      <div class="auth">
        <template v-if="isLoading">
          <span class="muted">Laddar …</span>
        </template>
        <template v-else-if="currentUser">
          <span class="muted">{{ currentUser.displayName }}</span>
          <button type="button" class="link-button" @click="logout">Logga ut</button>
        </template>
        <template v-else>
          <button type="button" class="login-button" @click="login">Logga in med Google</button>
        </template>
      </div>
    </div>
  </header>

  <main>
    <RouterView />
  </main>
</template>

<style scoped>
header {
  border-bottom: 1px solid var(--color-border);
  margin-bottom: 1.5rem;
  padding-bottom: 1rem;
}

.bar {
  display: flex;
  align-items: center;
  gap: 1rem;
  flex-wrap: wrap;
}

.brand {
  font-weight: 700;
  font-size: 1.1rem;
  color: var(--color-heading);
}

nav {
  display: flex;
  gap: 1rem;
  flex: 1;
}

nav a.router-link-active {
  font-weight: 600;
}

.auth {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.muted {
  color: var(--color-text);
  opacity: 0.7;
  font-size: 0.9rem;
}

.login-button {
  background: var(--color-accent);
  color: white;
  border: none;
  border-radius: 6px;
  padding: 0.5rem 0.9rem;
  cursor: pointer;
}

.link-button {
  background: none;
  border: none;
  color: var(--color-accent);
  cursor: pointer;
  padding: 0;
  text-decoration: underline;
}
</style>
