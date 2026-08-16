<template>
  <div class="recent page-container">
    <h1 class="page-title">近期阅读</h1>

    <div v-if="items.length" class="recent-list">
      <router-link
        v-for="item in items"
        :key="item.articleId"
        :to="`/reading/${item.articleId}`"
        class="recent-card"
      >
        <div class="recent-cover" :style="{ background: coverGradient(item.title) }">
          <span class="cover-initial">{{ coverInitial(item.title) }}</span>
          <span v-if="item.isCompleted" class="done-badge">已读完</span>
        </div>
        <div class="recent-body">
          <div class="recent-top">
            <h3 class="recent-title">{{ item.title }}</h3>
            <span class="recent-time">{{ formatRelativeTime(item.lastReadAt) }}</span>
          </div>
          <!-- 细进度条 -->
          <div class="progress-track">
            <div class="progress-fill" :style="{ width: item.progress + '%' }"></div>
          </div>
          <div class="recent-bottom">
            <span class="recent-percent">{{ item.progress }}%</span>
            <span class="diff-badge" :class="diffOf(item.difficulty).className">
              {{ diffOf(item.difficulty).label }}
            </span>
          </div>
        </div>
      </router-link>
    </div>

    <div v-else class="empty">
      <p class="empty-title">还没有阅读记录</p>
      <p class="empty-desc">去书架挑一篇开始精读吧</p>
      <router-link to="/" class="empty-link">前往书架 →</router-link>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { userApi, type RecentReadingItem } from '@/api/user'
import { coverGradient, coverInitial } from '@/utils/cover'
import { DIFFICULTY, formatRelativeTime } from '@/utils/format'

const items = ref<RecentReadingItem[]>([])

/** 难度徽章（number → 1|2|3 收窄） */
function diffOf(d: number) {
  return DIFFICULTY[d as 1 | 2 | 3]
}

onMounted(async () => {
  try {
    items.value = await userApi.recentReading()
  } catch {
    // 加载失败显示空态
  }
})
</script>

<style scoped>
.page-title {
  font-family: var(--font-serif-zh);
  font-size: 1.6rem;
  color: var(--ink);
  margin-bottom: var(--space-5);
}

.recent-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
}

.recent-card {
  display: flex;
  gap: var(--space-4);
  background: var(--bg-card);
  border: 1px solid var(--line);
  border-radius: var(--radius-md);
  padding: var(--space-4);
  color: var(--ink);
  transition: box-shadow var(--transition-base), transform var(--transition-base);
}

.recent-card:hover {
  box-shadow: var(--shadow);
  transform: translateY(-2px);
}

.recent-cover {
  position: relative;
  width: 110px;
  height: 74px;
  flex-shrink: 0;
  border-radius: var(--radius-sm);
  display: flex;
  align-items: center;
  justify-content: center;
}

.cover-initial {
  font-family: var(--font-serif-en);
  font-size: 1.8rem;
  font-weight: 700;
  color: rgba(255, 255, 255, 0.85);
}

.done-badge {
  position: absolute;
  top: 4px;
  right: 4px;
  padding: 1px 6px;
  border-radius: 999px;
  background: rgba(30, 38, 32, 0.75);
  color: #fff;
  font-size: 0.65rem;
}

.recent-body {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: var(--space-2);
}

.recent-top {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  gap: var(--space-3);
}

.recent-title {
  font-family: var(--font-serif-en), var(--font-serif-zh);
  font-size: 1rem;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.recent-time {
  color: var(--ink-3);
  font-size: 0.78rem;
  flex-shrink: 0;
}

/* 细进度条 */
.progress-track {
  height: 4px;
  border-radius: 2px;
  background: var(--bg-muted);
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  border-radius: 2px;
  background: var(--accent);
  transition: width 0.4s ease-out;
}

.recent-bottom {
  display: flex;
  align-items: center;
  gap: var(--space-2);
}

.recent-percent {
  font-size: 0.8rem;
  color: var(--ink-2);
}

.diff-badge {
  padding: 1px 8px;
  border-radius: 999px;
  font-size: 0.7rem;
}

.diff-1 {
  background: rgba(63, 90, 69, 0.12);
  color: var(--green);
}

.diff-2 {
  background: rgba(184, 134, 11, 0.12);
  color: var(--accent);
}

.diff-3 {
  background: rgba(179, 64, 46, 0.12);
  color: var(--danger);
}

.empty {
  text-align: center;
  padding: var(--space-7) 0;
  color: var(--ink-3);
}

.empty-title {
  font-family: var(--font-serif-zh);
  font-size: 1.2rem;
  color: var(--ink-2);
  margin-bottom: var(--space-2);
}

.empty-desc {
  font-size: 0.9rem;
  margin-bottom: var(--space-4);
}

.empty-link {
  color: var(--accent);
  border-bottom: 1px solid var(--accent);
}

@media (max-width: 767px) {
  .recent-cover {
    width: 84px;
    height: 60px;
  }

  .recent-time {
    display: none;
  }
}
</style>
