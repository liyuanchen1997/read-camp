<template>
  <div class="stats-page">
    <h2 class="page-title">仪表盘</h2>

    <div v-loading="loading" class="stat-grid">
      <div class="stat-card">
        <span class="stat-icon">👥</span>
        <span class="stat-num">{{ stats?.users ?? 0 }}</span>
        <span class="stat-label">注册用户</span>
      </div>
      <div class="stat-card">
        <span class="stat-icon">📚</span>
        <span class="stat-num">{{ stats?.articles ?? 0 }}</span>
        <span class="stat-label">文章总数</span>
      </div>
      <div class="stat-card">
        <span class="stat-icon">🟢</span>
        <span class="stat-num">{{ stats?.published ?? 0 }}</span>
        <span class="stat-label">已上架</span>
      </div>
      <div class="stat-card">
        <span class="stat-icon">✏️</span>
        <span class="stat-num">{{ stats?.sentences ?? 0 }}</span>
        <span class="stat-label">句子总数</span>
      </div>
      <div class="stat-card">
        <span class="stat-icon">🤖</span>
        <span class="stat-num">{{ stats?.genDone ?? 0 }}</span>
        <span class="stat-label">已生成标注句</span>
      </div>
      <div class="stat-card">
        <span class="stat-icon">⚠️</span>
        <span class="stat-num failed">{{ stats?.genFailed ?? 0 }}</span>
        <span class="stat-label">生成失败句</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { adminApi, type AdminStats } from '@/api/admin'

const stats = ref<AdminStats | null>(null)
const loading = ref(true)

onMounted(async () => {
  try {
    stats.value = await adminApi.stats()
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.page-title {
  font-family: var(--font-serif-zh);
  font-size: 1.3rem;
  margin-bottom: var(--space-4);
}

.stat-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: var(--space-4);
}

.stat-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  background: var(--bg-card);
  border: 1px solid var(--line);
  border-radius: var(--radius-md);
  padding: var(--space-5) var(--space-4);
}

.stat-icon {
  font-size: 1.4rem;
}

.stat-num {
  font-family: var(--font-serif-en);
  font-size: 1.8rem;
  font-weight: 700;
  color: var(--accent);
  font-variant-numeric: tabular-nums;
}

.stat-num.failed {
  color: var(--danger);
}

.stat-label {
  font-size: 0.82rem;
  color: var(--ink-2);
}
</style>
