<script setup lang="ts">
import { ref } from 'vue'

const props = withDefaults(
  defineProps<{
    modelValue: number
    interactive?: boolean
    size?: 'sm' | 'md'
  }>(),
  { interactive: false, size: 'md' },
)

const emit = defineEmits<{
  'update:modelValue': [value: number]
}>()

const hoverValue = ref<number | null>(null)

function select(value: number) {
  if (props.interactive) {
    emit('update:modelValue', value)
  }
}
</script>

<template>
  <span
    class="star-rating"
    :class="[{ interactive }, size]"
    role="img"
    :aria-label="`Betyg: ${modelValue} av 5 stjärnor`"
  >
    <button
      v-for="value in 5"
      :key="value"
      type="button"
      class="star"
      :class="{ filled: value <= (hoverValue ?? modelValue) }"
      :disabled="!interactive"
      :aria-label="`${value} ${value === 1 ? 'stjärna' : 'stjärnor'}`"
      @click="select(value)"
      @mouseenter="interactive && (hoverValue = value)"
      @mouseleave="interactive && (hoverValue = null)"
    >
      ★
    </button>
  </span>
</template>

<style scoped>
.star-rating {
  display: inline-flex;
  gap: 0.125rem;
}

.star {
  background: none;
  border: none;
  padding: 0;
  font-size: 1.25rem;
  line-height: 1;
  color: var(--color-star-empty, #d0d0d0);
  cursor: default;
}

.star.filled {
  color: var(--color-star-filled, #f5a623);
}

.star-rating.interactive .star {
  cursor: pointer;
}

.star-rating.interactive .star:hover {
  transform: scale(1.1);
}

.star-rating.sm .star {
  font-size: 0.85rem;
}
</style>
