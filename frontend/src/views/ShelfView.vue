<template>
  <div class="shelf page-container">
    <div class="shelf-head">
      <h1 class="shelf-title">书架</h1>
      <p class="shelf-subtitle">挑一本，开始今天的精读</p>
    </div>

    <!-- 筛选栏 -->
    <div class="filter-bar">
      <input
        v-model.trim="keyword"
        class="search-input"
        type="search"
        placeholder="搜索文章标题…"
        @keyup.enter="reload"
      />
      <div class="difficulty-filter" role="group" aria-label="难度筛选">
        <button
          v-for="opt in difficultyOptions"
          :key="opt.value ?? 'all'"
          class="filter-chip"
          :class="{ active: difficulty === opt.value }"
          @click="setDifficulty(opt.value)"
        >
          {{ opt.label }}
        </button>
      </div>
      <button class="search-btn" @click="reload">搜索</button>
    </div>

    <!-- 文章网格 -->
    <div v-if="articles.length" class="grid">
      <router-link
        v-for="article in articles"
        :key="article.id"
        :to="`/reading/${article.id}`"
        class="card"
      >
        <div class="cover" :style="{ background: coverGradient(article.title) }">
          <span class="cover-initial">{{ coverInitial(article.title) }}</span>
          <span v-if="progressMap[article.id]" class="progress-badge">
            {{ progressMap[article.id].progress }}%
          </span>
        </div>
        <div class="card-body">
          <h3 class="card-title">{{ article.title }}</h3>
          <p class="card-summary">{{ article.summary || '暂无简介' }}</p>
          <div class="card-meta">
            <span class="diff-badge" :class="DIFFICULTY[article.difficulty].className">
              {{ DIFFICULTY[article.difficulty].label }}
            </span>
            <span class="meta-text">{{ article.wordCount }} 词 · {{ article.sentenceCount }} 句</span>
          </div>
        </div>
      </router-link>
    </div>

    <!-- 空态 -->
    <div v-else-if="!loading" class="empty">
      <p class="empty-title">书架空空如也</p>
      <p class="empty-desc">{{ keyword || difficulty ? '没有找到符合条件的文章' : '等待上架新文章' }}</p>
    </div>

    <div v-if="loading" class="loading-tip">加载中…</div>
    <div v-else-if="hasMore" class="load-more-wrap">
      <button class="load-more" @click="loadMore">加载更多</button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { articleApi, type ArticleDto } from '@/api/article'
import { userApi } from '@/api/user'
import { useUserStore } from '@/stores/user'
import { coverGradient, coverInitial } from '@/utils/cover'
import { DIFFICULTY } from '@/utils/format'

const userStore = useUserStore()

const articles = ref<ArticleDto[]>([])
const keyword = ref('')
const difficulty = ref<number | null>(null)
const page = ref(1)
const total = ref(0)
const loading = ref(false)

/** 登录用户已读文章的进度（角标数据源，来自近期阅读） */
const progressMap = ref<Record<number, { progress: number; isCompleted: boolean }>>({})

const difficultyOptions = [
  { value: null, label: '全部' },
  { value: 1, label: '入门' },
  { value: 2, label: '进阶' },
  { value: 3, label: '挑战' },
]

const hasMore = computed(() => articles.value.length < total.value)

function setDifficulty(value: number | null) {
  if (difficulty.value !== value) {
    difficulty.value = value
    reload()
  }
}

async function fetchPage(p: number) {
  loading.value = true
  try {
    const data = await articleApi.shelf({
      keyword: keyword.value || undefined,
      difficulty: difficulty.value ?? undefined,
      page: p,
      size: 12,
    })
    total.value = data.total
    articles.value = p === 1 ? data.records : [...articles.value, ...data.records]
    page.value = p
  } catch {
    // 错误提示：保持现有列表
  } finally {
    loading.value = false
  }
}

function reload() {
  fetchPage(1)
}

function loadMore() {
  fetchPage(page.value + 1)
}

/** 登录后拉取近期阅读（最近 10 篇）作为进度角标数据源 */
async function loadProgressBadges() {
  if (!userStore.isLoggedIn()) return
  try {
    const recent = await userApi.recentReading()
    const map: Record<number, { progress: number; isCompleted: boolean }> = {}
    for (const item of recent) {
      map[item.articleId] = { progress: item.progress, isCompleted: item.isCompleted }
    }
    progressMap.value = map
  } catch {
    // 忽略：无角标不影响浏览
  }
}

onMounted(() => {
  fetchPage(1)
  loadProgressBadges()
})

// 登录状态变化（如登录后返回书架）时刷新角标
watch(() => userStore.token, loadProgressBadges)
</script>

<style scoped>
.shelf {
  padding-top: var(--space-6);
  padding-bottom: var(--space-7);
}

.shelf-head {
  margin-bottom: var(--space-5);
}

.shelf-title {
  font-family: var(--font-serif-zh);
  font-size: 1.6rem;
  color: var(--ink);
}

.shelf-subtitle {
  color: var(--ink-3);
  font-size: 0.9rem;
  margin-top: var(--space-1);
}

.filter-bar {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  margin-bottom: var(--space-5);
  flex-wrap: wrap;
}

.search-input {
  flex: 1;
  min-width: 200px;
  padding: 9px 12px;
  border: 1px solid var(--line);
  border-radius: var(--radius-sm);
  background: var(--bg-card);
  color: var(--ink);
  font-size: 0.92rem;
}

.search-input:focus {
  outline: none;
  border-color: var(--accent);
}

.difficulty-filter {
  display: flex;
  gap: var(--space-2);
}

.filter-chip {
  padding: 7px 14px;
  border: 1px solid var(--line);
  border-radius: 999px;
  background: transparent;
  color: var(--ink-2);
  font-size: 0.85rem;
  cursor: pointer;
  transition: all var(--transition-fast);
}

.filter-chip:hover {
  color: var(--accent);
  border-color: var(--accent);
}

.filter-chip.active {
  background: var(--accent);
  border-color: var(--accent);
  color: #fff;
}

.search-btn {
  padding: 8px 18px;
  border: none;
  border-radius: var(--radius-sm);
  background: var(--accent);
  color: #fff;
  font-size: 0.9rem;
  cursor: pointer;
  transition: background var(--transition-fast);
}

.search-btn:hover {
  background: var(--accent-hover);
}

/* 网格 */
.grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: var(--space-5);
}

.card {
  display: flex;
  flex-direction: column;
  background: var(--bg-card);
  border: 1px solid var(--line);
  border-radius: var(--radius-md);
  overflow: hidden;
  color: var(--ink);
  transition: transform var(--transition-base), box-shadow var(--transition-base);
}

.card:hover {
  transform: translateY(-3px);
  box-shadow: var(--shadow);
}

.cover {
  position: relative;
  height: 120px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.cover-initial {
  font-family: var(--font-serif-en);
  font-size: 3.2rem;
  font-weight: 700;
  color: rgba(255, 255, 255, 0.85);
}

.progress-badge {
  position: absolute;
  top: 8px;
  right: 8px;
  padding: 2px 8px;
  border-radius: 999px;
  background: rgba(30, 38, 32, 0.75);
  color: #fff;
  font-size: 0.75rem;
}

.card-body {
  padding: var(--space-4);
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
  flex: 1;
}

.card-title {
  font-family: var(--font-serif-en), var(--font-serif-zh);
  font-size: 1.05rem;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.card-summary {
  color: var(--ink-3);
  font-size: 0.82rem;
  line-height: 1.6;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  flex: 1;
}

.card-meta {
  display: flex;
  align-items: center;
  gap: var(--space-2);
}

.diff-badge {
  padding: 2px 8px;
  border-radius: 999px;
  font-size: 0.72rem;
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

.meta-text {
  color: var(--ink-3);
  font-size: 0.75rem;
}

/* 空态与加载 */
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
}

.loading-tip {
  text-align: center;
  color: var(--ink-3);
  padding: var(--space-5);
}

.load-more-wrap {
  text-align: center;
  padding: var(--space-5) 0 var(--space-2);
}

.load-more {
  padding: 9px 28px;
  border: 1px solid var(--line);
  border-radius: 999px;
  background: transparent;
  color: var(--ink-2);
  font-size: 0.9rem;
  cursor: pointer;
  transition: all var(--transition-fast);
}

.load-more:hover {
  color: var(--accent);
  border-color: var(--accent);
}

@media (max-width: 767px) {
  .grid {
    grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
    gap: var(--space-4);
  }

  .cover {
    height: 90px;
  }

  .cover-initial {
    font-size: 2.2rem;
  }

  .card-body {
    padding: var(--space-3);
  }

  .card-title {
    font-size: 0.92rem;
  }
}
</style>
