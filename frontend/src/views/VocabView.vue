<template>
  <div class="vocab page-container">
    <h1 class="page-title">生词本</h1>

    <div class="search-bar">
      <input
        v-model.trim="keyword"
        class="search-input"
        type="search"
        placeholder="搜索单词…"
        @keyup.enter="reload"
      />
      <button class="search-btn" @click="reload">搜索</button>
    </div>

    <div v-if="items.length" class="vocab-list">
      <div v-for="item in items" :key="item.id" class="vocab-item">
        <div class="vocab-main">
          <div class="vocab-head">
            <h3 class="vocab-word">{{ item.word }}</h3>
            <span v-if="item.pos" class="vocab-pos">{{ item.pos }}</span>
            <button class="speak-btn" title="发音" @click="tts.speak(item.word)">🔊</button>
          </div>
          <p v-if="item.meaning" class="vocab-meaning">{{ item.meaning }}</p>
          <p v-if="item.role" class="vocab-role">{{ item.role }}</p>
          <p v-if="item.contextSentence" class="vocab-context">{{ item.contextSentence }}</p>
        </div>
        <div class="vocab-side">
          <span class="vocab-time">{{ formatRelativeTime(item.createdAt) }}</span>
          <button class="remove-btn" title="删除" @click="remove(item)">✕</button>
        </div>
      </div>
    </div>

    <div v-else-if="!loading" class="empty">
      <p class="empty-title">{{ keyword ? '没有找到匹配的单词' : '生词本还是空的' }}</p>
      <p class="empty-desc">阅读时点击气泡中的单词即可加入生词本</p>
    </div>

    <div v-if="loading" class="loading-tip">加载中…</div>
    <div v-else-if="hasMore" class="load-more-wrap">
      <button class="load-more" @click="loadMore">加载更多</button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { vocabApi, type VocabItem } from '@/api/vocab'
import { tts } from '@/services/tts'
import { formatRelativeTime } from '@/utils/format'

const items = ref<VocabItem[]>([])
const keyword = ref('')
const page = ref(1)
const total = ref(0)
const loading = ref(false)

const hasMore = computed(() => items.value.length < total.value)

async function fetchPage(p: number) {
  loading.value = true
  try {
    const data = await vocabApi.list({ keyword: keyword.value || undefined, page: p, size: 20 })
    total.value = data.total
    items.value = p === 1 ? data.records : [...items.value, ...data.records]
    page.value = p
  } catch {
    // 保持现状
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

async function remove(item: VocabItem) {
  try {
    await vocabApi.remove(item.word)
    items.value = items.value.filter((it) => it.id !== item.id)
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

.search-bar {
  display: flex;
  gap: var(--space-3);
  margin-bottom: var(--space-5);
  max-width: 420px;
}

.search-input {
  flex: 1;
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

.search-btn {
  padding: 8px 18px;
  border: none;
  border-radius: var(--radius-sm);
  background: var(--accent);
  color: #fff;
  font-size: 0.9rem;
  cursor: pointer;
}

.vocab-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}

.vocab-item {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--space-3);
  background: var(--bg-card);
  border: 1px solid var(--line);
  border-radius: var(--radius-sm);
  padding: var(--space-4);
}

.vocab-main {
  min-width: 0;
}

.vocab-head {
  display: flex;
  align-items: baseline;
  gap: var(--space-2);
}

.vocab-word {
  font-family: var(--font-serif-en);
  font-size: 1.15rem;
  color: var(--accent);
}

.vocab-pos {
  font-size: 0.75rem;
  color: var(--ink-3);
  font-style: italic;
}

.speak-btn {
  margin-left: auto;
  border: none;
  background: transparent;
  color: var(--ink-3);
  cursor: pointer;
  font-size: 0.9rem;
  padding: 2px 6px;
  border-radius: 6px;
  transition: all var(--transition-fast);
}

.speak-btn:hover {
  color: var(--accent);
  background: var(--bg-hover);
}

.vocab-meaning {
  color: var(--ink);
  font-size: 0.95rem;
  margin-top: var(--space-1);
}

.vocab-role {
  color: var(--ink-2);
  font-size: 0.82rem;
  margin-top: 2px;
  line-height: 1.6;
}

.vocab-context {
  color: var(--ink-3);
  font-size: 0.82rem;
  margin-top: var(--space-2);
  line-height: 1.6;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.vocab-side {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  flex-shrink: 0;
}

.vocab-time {
  color: var(--ink-3);
  font-size: 0.75rem;
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
