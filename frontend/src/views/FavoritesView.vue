<template>
  <div class="favorites page-container">
    <h1 class="page-title">例句收藏</h1>

    <div v-if="items.length" class="fav-list">
      <div v-for="item in items" :key="item.sentenceId" class="fav-item">
        <div class="fav-en-row">
          <p class="fav-en">{{ item.en }}</p>
          <button class="speak-btn" title="朗读本句" @click="tts.speak(item.en)">🔊</button>
        </div>
        <p v-if="item.zh" class="fav-zh">{{ item.zh }}</p>
        <p v-if="item.explanation" class="fav-expl">{{ item.explanation }}</p>
        <!-- 可收起的单词列表（默认收起） -->
        <div v-if="item.words?.length" class="fav-words">
          <button class="fav-words-toggle" @click="toggleWords(item.sentenceId)">
            {{ expandedWords.has(item.sentenceId) ? '▾ 收起单词' : '▸ 查看单词' }}
          </button>
          <div v-if="expandedWords.has(item.sentenceId)" class="fav-words-list">
            <span v-for="(w, i) in item.words" :key="i" class="fw-item">
              <span class="fw-word">{{ w.word }}</span>
              <span v-if="w.phonetic" class="fw-phonetic">{{ w.phonetic }}</span>
              <span v-if="w.pos" class="fw-pos">{{ w.pos }}</span>
              <span v-if="w.meaning" class="fw-meaning">{{ w.meaning }}</span>
            </span>
          </div>
        </div>
        <div class="fav-meta">
          <router-link :to="`/reading/${item.articleId}`" class="fav-article">
            {{ item.articleTitle }} · 第 {{ item.seq + 1 }} 句
          </router-link>
          <span class="fav-time">{{ formatRelativeTime(item.createdAt) }}</span>
          <button class="remove-btn" title="取消收藏" @click="remove(item.sentenceId)">✕</button>
        </div>
      </div>
    </div>

    <div v-else-if="!loading" class="empty">
      <p class="empty-title">还没有收藏例句</p>
      <p class="empty-desc">阅读时点击句子的气泡，点 ❤ 即可收藏</p>
    </div>

    <div v-if="loading" class="loading-tip">加载中…</div>
    <div v-else-if="hasMore" class="load-more-wrap">
      <button class="load-more" @click="loadMore">加载更多</button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { favoriteApi, type FavoriteItem } from '@/api/favorite'
import { tts } from '@/services/tts'
import { formatRelativeTime } from '@/utils/format'

const items = ref<FavoriteItem[]>([])
const page = ref(1)
const total = ref(0)
const loading = ref(false)

/** 展开单词列表的句子 id 集合 */
const expandedWords = ref<Set<number>>(new Set())

function toggleWords(sentenceId: number) {
  const next = new Set(expandedWords.value)
  if (next.has(sentenceId)) next.delete(sentenceId)
  else next.add(sentenceId)
  expandedWords.value = next
}

const hasMore = computed(() => items.value.length < total.value)

async function fetchPage(p: number) {
  loading.value = true
  try {
    const data = await favoriteApi.list({ page: p, size: 20 })
    total.value = data.total
    items.value = p === 1 ? data.records : [...items.value, ...data.records]
    page.value = p
  } catch {
    // 保持现状
  } finally {
    loading.value = false
  }
}

function loadMore() {
  fetchPage(page.value + 1)
}

async function remove(sentenceId: number) {
  try {
    await favoriteApi.remove(sentenceId)
    items.value = items.value.filter((it) => it.sentenceId !== sentenceId)
    total.value -= 1
  } catch {
    // 忽略
  }
}

onMounted(() => fetchPage(1))
</script>

<style scoped>
.page-title {
  font-family: var(--font-serif-zh);
  font-size: 1.6rem;
  color: var(--ink);
  margin-bottom: var(--space-5);
}

.fav-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}

.fav-item {
  background: var(--bg-card);
  border: 1px solid var(--line);
  border-radius: var(--radius-sm);
  padding: var(--space-4);
}

.fav-en-row {
  display: flex;
  align-items: flex-start;
  gap: var(--space-2);
}

.fav-en {
  font-family: var(--font-serif-en);
  font-size: 1rem;
  line-height: 1.8;
  color: var(--ink);
  flex: 1;
}

.speak-btn {
  border: none;
  background: transparent;
  color: var(--ink-3);
  cursor: pointer;
  font-size: 0.9rem;
  padding: 2px 6px;
  border-radius: 6px;
  transition: all var(--transition-fast);
  flex-shrink: 0;
}

.speak-btn:hover {
  color: var(--accent);
  background: var(--bg-hover);
}

.fav-zh {
  font-family: var(--font-serif-zh);
  color: var(--ink-2);
  font-size: 0.9rem;
  line-height: 1.8;
  margin-top: var(--space-1);
}

.fav-expl {
  color: var(--ink-3);
  font-size: 0.82rem;
  line-height: 1.7;
  margin-top: var(--space-1);
}

/* 可收起单词列表 */
.fav-words {
  margin-top: var(--space-2);
}

.fav-words-toggle {
  border: none;
  background: transparent;
  color: var(--accent);
  font-size: 0.8rem;
  cursor: pointer;
  padding: 0;
}

.fav-words-list {
  display: flex;
  flex-wrap: wrap;
  gap: 6px 12px;
  margin-top: var(--space-2);
  padding: var(--space-2) var(--space-3);
  border: 1px solid var(--line);
  border-radius: var(--radius-sm);
  background: var(--bg-hover);
}

.fw-item {
  display: inline-flex;
  align-items: baseline;
  gap: 4px;
  font-size: 0.82rem;
}

.fw-word {
  font-family: var(--font-serif-en);
  font-weight: 600;
  color: var(--accent);
}

.fw-phonetic {
  font-size: 0.72rem;
  color: var(--ink-3);
  font-family: var(--font-serif-en);
}

.fw-pos {
  font-size: 0.72rem;
  color: var(--ink-3);
  font-style: italic;
}

.fw-meaning {
  color: var(--ink-2);
}

.fav-meta {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  margin-top: var(--space-2);
}

.fav-article {
  color: var(--accent);
  font-size: 0.8rem;
}

.fav-time {
  color: var(--ink-3);
  font-size: 0.75rem;
  flex: 1;
}

.remove-btn {
  width: 26px;
  height: 26px;
  border: 1px solid var(--line);
  border-radius: 50%;
  background: transparent;
  color: var(--ink-3);
  cursor: pointer;
  transition: all var(--transition-fast);
}

.remove-btn:hover {
  border-color: var(--danger);
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
}

.loading-tip {
  text-align: center;
  color: var(--ink-3);
  padding: var(--space-5);
}

.load-more-wrap {
  text-align: center;
  padding: var(--space-5) 0;
}

.load-more {
  padding: 9px 28px;
  border: 1px solid var(--line);
  border-radius: 999px;
  background: transparent;
  color: var(--ink-2);
  font-size: 0.9rem;
  cursor: pointer;
}

.load-more:hover {
  color: var(--accent);
  border-color: var(--accent);
}
</style>
