<script setup lang="ts">
import { onMounted, onUnmounted, ref } from 'vue'
import { RouterLink, RouterView, useRouter } from 'vue-router'
import { resolveImageUrl } from '@/api/client'
import { useAuth } from '@/composables/useAuth'

const router = useRouter()
const { currentUser, logout } = useAuth()
const currentYear = new Date().getFullYear()

const menuOpen = ref(false)
const menuRef = ref<HTMLElement | null>(null)

function toggleMenu() {
  menuOpen.value = !menuOpen.value
}

function closeMenu() {
  menuOpen.value = false
}

async function handleLogout() {
  closeMenu()
  await logout()
  router.push('/feed')
}

function onDocumentClick(event: MouseEvent) {
  if (menuOpen.value && menuRef.value && !menuRef.value.contains(event.target as Node)) {
    menuOpen.value = false
  }
}

onMounted(() => document.addEventListener('click', onDocumentClick))
onUnmounted(() => document.removeEventListener('click', onDocumentClick))
</script>

<template>
  <header>
    <div class="bar">
      <RouterLink to="/feed" class="brand">receptfeed</RouterLink>
      <nav v-if="currentUser">
        <RouterLink to="/add-review" class="add-button" title="Recensera ett recept" aria-label="Recensera ett recept">
          +
        </RouterLink>
      </nav>

      <div v-if="currentUser" ref="menuRef" class="profile">
        <button
          type="button"
          class="profile-trigger"
          :class="{ open: menuOpen }"
          title="Konto"
          aria-label="Öppna kontomeny"
          @click="toggleMenu"
        >
          <img v-if="currentUser.avatarUrl" :src="resolveImageUrl(currentUser.avatarUrl) ?? ''" alt="" />
          <svg
            v-else
            class="avatar-fallback"
            viewBox="0 0 24 24"
            width="22"
            height="22"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
            stroke-linecap="round"
            stroke-linejoin="round"
          >
            <circle cx="12" cy="8" r="4" />
            <path d="M4 20c0-4 3.5-7 8-7s8 3 8 7" />
          </svg>
        </button>

        <div v-if="menuOpen" class="dropdown">
          <p class="dropdown-name">{{ currentUser.displayName }}</p>
          <RouterLink to="/konto" class="dropdown-item" @click="closeMenu">Konto</RouterLink>
          <RouterLink to="/sparat" class="dropdown-item" @click="closeMenu">Sparat</RouterLink>
          <RouterLink to="/hjalp" class="dropdown-item" @click="closeMenu">Hjälp</RouterLink>
          <button type="button" class="dropdown-item logout" @click="handleLogout">Logga ut</button>
        </div>
      </div>
    </div>
  </header>

  <main>
    <RouterView />
  </main>

  <footer class="site-footer">
    <span class="footer-brand">receptfeed</span>
    <span class="copyright">© {{ currentYear }}</span>
  </footer>
</template>

<style scoped>
header {
  position: relative;
  z-index: 10;
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
  align-items: center;
  gap: 1rem;
  flex: 1;
}

nav a.router-link-active {
  font-weight: 600;
}

.add-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 1.6rem;
  height: 1.6rem;
  border-radius: 50%;
  background: var(--color-accent);
  color: white;
  font-size: 1.1rem;
  line-height: 1;
  text-decoration: none;
}

.add-button:hover {
  text-decoration: none;
  opacity: 0.88;
}

.profile {
  position: relative;
}

.profile-trigger {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 2.1rem;
  height: 2.1rem;
  border-radius: 50%;
  border: 1px solid var(--color-border);
  background: var(--color-background-mute);
  overflow: hidden;
  padding: 0;
  cursor: pointer;
}

.profile-trigger.open,
.profile-trigger:hover {
  border-color: var(--color-border-hover);
}

.profile-trigger img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.avatar-fallback {
  color: #9a9a9a;
}

.dropdown {
  position: absolute;
  z-index: 20;
  right: 0;
  top: calc(100% + 0.6rem);
  min-width: 11rem;
  background: var(--color-background);
  border: 1px solid var(--color-border);
  border-radius: 10px;
  padding: 0.4rem;
  box-shadow: 0 8px 24px rgba(46, 36, 23, 0.14);
}

.dropdown-name {
  padding: 0.4rem 0.6rem 0.5rem;
  margin: 0 0 0.3rem;
  border-bottom: 1px solid var(--color-border);
  font-size: 0.85rem;
  font-weight: 600;
  color: var(--color-heading);
}

.dropdown-item {
  display: block;
  width: 100%;
  padding: 0.45rem 0.6rem;
  border: none;
  border-radius: 6px;
  background: none;
  color: var(--color-text);
  font: inherit;
  font-size: 0.9rem;
  text-align: left;
  text-decoration: none;
  cursor: pointer;
}

.dropdown-item:hover {
  background: var(--color-background-mute);
  text-decoration: none;
}

.dropdown-item.logout {
  margin-top: 0.3rem;
  padding-top: 0.55rem;
  border-top: 1px solid var(--color-border);
  border-radius: 0 0 6px 6px;
}

.site-footer {
  margin-top: 3rem;
  padding-top: 1.5rem;
  border-top: 1px solid var(--color-border);
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 0.8rem;
  color: var(--color-text);
  opacity: 0.6;
}

.footer-brand {
  font-weight: 700;
}
</style>
