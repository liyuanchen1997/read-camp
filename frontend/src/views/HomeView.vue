<template>
  <div class="home page-container">
    <h1 class="brand">英语精读训练营</h1>
    <p class="slogan">一间安静的线上阅览室</p>
    <p class="hint">项目骨架已就绪（步骤 1），页面开发按计划表逐步进行。</p>
  </div>
</template>

<script setup lang="ts">
// 占位首页，步骤 6-7 替换为真实页面
</script>

<style scoped>
.home {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  gap: var(--space-3);
}

.brand {
  font-family: var(--font-serif-zh);
  font-size: 2.2rem;
  color: var(--accent);
  letter-spacing: 0.05em;
}

.slogan {
  font-family: var(--font-serif-zh);
  color: var(--ink-2);
  font-size: 1.1rem;
}

.hint {
  color: var(--ink-3);
  font-size: 0.9rem;
}
</style>
