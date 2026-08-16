<template>
  <div class="placeholder page-container">
    <h2 class="ph-title">{{ title }}</h2>
    <p class="ph-desc">{{ desc }}</p>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  title: string
  desc: string
}>()
</script>

<style scoped>
.placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 60vh;
  gap: var(--space-3);
  text-align: center;
}

.ph-title {
  font-family: var(--font-serif-zh);
  color: var(--ink);
}

.ph-desc {
  color: var(--ink-3);
}
</style>
