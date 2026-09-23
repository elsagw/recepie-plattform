<script setup lang="ts">
withDefaults(defineProps<{ size?: 'sm' | 'md' }>(), { size: 'md' })
</script>

<template>
  <span class="spinner" :class="size" role="status" aria-label="Laddar"></span>
</template>

<style scoped>
.spinner {
  display: inline-block;
  border: 3px solid var(--color-border);
  border-top-color: var(--color-heading);
  border-radius: 50%;
  animation: spin 0.7s linear infinite;
}

.spinner.md {
  width: 1.5rem;
  height: 1.5rem;
}

.spinner.sm {
  width: 1rem;
  height: 1rem;
  border-width: 2px;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}
</style>
