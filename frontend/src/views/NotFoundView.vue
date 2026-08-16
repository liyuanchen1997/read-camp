<template>
  <div class="nf page-container">
    <h1 class="nf-code">404</h1>
    <p class="nf-desc">页面不存在或已被移除</p>
    <router-link to="/" class="nf-back">返回书架</router-link>
  </div>
</template>

<style scoped>
.nf {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 60vh;
  gap: var(--space-3);
}

.nf-code {
  font-family: var(--font-serif-en);
  font-size: 4rem;
  color: var(--accent);
}

.nf-desc {
  color: var(--ink-2);
}

.nf-back {
  color: var(--accent);
  border-bottom: 1px solid var(--accent);
}
</style>
