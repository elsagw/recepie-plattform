<script setup lang="ts"></script>

<template>
  <span class="skeleton" role="presentation" aria-hidden="true"></span>
</template>

<style scoped>
.skeleton {
  display: block;
  background: var(--color-background-mute);
  border-radius: 4px;
  animation: skeleton-pulse 1.4s ease-in-out infinite;
}

@keyframes skeleton-pulse {
  0%,
  100% {
    opacity: 1;
  }
  50% {
    opacity: 0.45;
  }
}

@media (prefers-reduced-motion: reduce) {
  .skeleton {
    animation: none;
    opacity: 0.7;
  }
}
</style>
