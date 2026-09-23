<script setup lang="ts">
const questions = [
  {
    question: 'Hur lägger jag till en recension?',
    answer: 'Klicka på plus-knappen i headern, klistra in en receptlänk och fyll i ditt betyg.',
  },
  {
    question: 'Kan jag ändra eller ta bort en recension?',
    answer: 'Ja, öppna recensionen från feedet så hittar du redigera och ta bort där.',
  },
  {
    question: 'Hur sparar jag ett recept till min lista?',
    answer: 'Klicka på hjärtat på receptbilden i feedet.',
  },
]
</script>

<template>
  <section class="narrow">
    <h1>Hjälp</h1>
    <p class="intro">Vanliga frågor om receptfeed.</p>

    <div class="faq">
      <div v-for="item in questions" :key="item.question" class="faq-item">
        <h2>{{ item.question }}</h2>
        <p>{{ item.answer }}</p>
      </div>
    </div>
  </section>
</template>

<style scoped>
h1 {
  font-family: var(--font-display);
  font-weight: 600;
  font-size: 1.75rem;
  color: var(--color-heading);
  margin: 0 0 0.3rem;
}

.intro {
  font-size: 0.95rem;
  color: var(--color-text);
  opacity: 0.7;
  margin: 0 0 2rem;
}

.faq {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.faq-item h2 {
  font-size: 1rem;
  font-weight: 600;
  color: var(--color-heading);
  margin: 0 0 0.35rem;
}

.faq-item p {
  font-size: 0.9rem;
  line-height: 1.6;
  color: var(--color-text);
  opacity: 0.8;
  margin: 0;
}
</style>
