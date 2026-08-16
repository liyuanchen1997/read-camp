<template>
  <div class="reading-page">
    <ArticleToolbar
      :article="article"
      :progress="store.progress"
      :show-zh="showZh"
      @toggle-zh="showZh = !showZh"
    >
      <template #actions>
        <!-- 朗读控制 -->
        <div class="tts-controls">
          <button
            class="toolbar-btn tts-btn"
            :title="ttsState === 'playing' ? '暂停' : '朗读全文'"
            @click="playOrResume"
          >
            {{ ttsState === 'playing' ? '⏸' : ttsState === 'paused' ? '▶︎' : '▶' }}
          </button>
          <button class="toolbar-btn tts-btn" title="停止" @click="stopTts">⏹</button>
          <input
            v-model.number="ttsRate"
            class="rate-slider"
            type="range"
            min="0.5"
            max="1.5"
            step="0.1"
            title="语速"
          />
          <span class="rate-label">{{ ttsRate.toFixed(1) }}x</span>
        </div>
      </template>
    </ArticleToolbar>

    <div class="reading-layout" :class="{ 'show-zh': showZh }">
      <SentencePane
        ref="enPaneRef"
        side="en"
        :sentences="store.sentences"
        @sentence-click="onSentenceClick"
      />
      <SentencePane
        v-show="showZh"
        ref="zhPaneRef"
        side="zh"
        :sentences="store.sentences"
        @sentence-click="onSentenceClick"
      />
    </div>

    <!-- 句子气泡 -->
    <SentenceBubble
      v-if="bubble?.type === 'sentence'"
      :sentence="bubble.sentence"
      :anchor="bubble.anchor"
      @close="closeBubble"
      @word-click="onWordClick"
    />
    <!-- 单词气泡 -->
    <WordBubble
      v-if="bubble?.type === 'word'"
      :word="bubble.word"
      :anchor="bubble.anchor"
      :source-article-id="store.articleId"
      :context-sentence="wordContextSentence"
      @close="closeBubble"
    />

    <div v-if="loading" class="reading-loading">
      <p>正在打开文章…</p>
    </div>
    <div v-else-if="errorMsg" class="reading-error">
      <p>{{ errorMsg }}</p>
      <router-link to="/" class="back-link">返回书架</router-link>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { articleApi, type ArticleDto, type SentenceDto } from '@/api/article'
import ArticleToolbar from '@/components/reading/ArticleToolbar.vue'
import SentenceBubble from '@/components/reading/SentenceBubble.vue'
import SentencePane from '@/components/reading/SentencePane.vue'
import WordBubble from '@/components/reading/WordBubble.vue'
import { useReadTracking } from '@/composables/useReadTracking'
import { useScrollSync } from '@/composables/useScrollSync'
import { tts } from '@/services/tts'
import { useReadingStore } from '@/stores/reading'
import '@/styles/reading.css'

type BubbleState =
  | { type: 'sentence'; sentence: SentenceDto; anchor: DOMRect }
  | { type: 'word'; word: { word: string; pos?: string; meaning?: string; role?: string }; anchor: DOMRect }
  | null

const route = useRoute()
const store = useReadingStore()

const article = ref<ArticleDto | null>(null)
const showZh = ref(false)
const loading = ref(true)
const errorMsg = ref('')
const bubble = ref<BubbleState>(null)
const wordContextSentence = ref('')

const ttsState = ref(tts.getState())
const ttsRate = ref(tts.getRate())

const enPaneRef = ref<InstanceType<typeof SentencePane> | null>(null)
const zhPaneRef = ref<InstanceType<typeof SentencePane> | null>(null)

const enScroller = computed(() => enPaneRef.value?.scrollerEl ?? null)
const zhScroller = computed(() => zhPaneRef.value?.scrollerEl ?? null)

/** 桌面双栏模式（≥1024px）才启用同步滚动与双栏观察 */
const isDesktop = ref(window.matchMedia('(min-width: 1024px)').matches)
const mql = window.matchMedia('(min-width: 1024px)')

const scrollSync = useScrollSync(enScroller, zhScroller)
const tracking = useReadTracking(Number(route.params.id))

// ---------- 气泡 ----------

function onSentenceClick(sentence: SentenceDto, event: MouseEvent) {
  const el = (event.target as HTMLElement).closest('.sentence')
  const anchor = el?.getBoundingClientRect() ?? new DOMRect(0, 0, 0, 0)
  bubble.value = { type: 'sentence', sentence, anchor }
}

function onWordClick(
  word: { word: string; pos?: string; meaning?: string; role?: string },
  event: MouseEvent,
) {
  const el = (event.target as HTMLElement).closest('.word-item')
  const anchor = el?.getBoundingClientRect() ?? new DOMRect(0, 0, 0, 0)
  if (bubble.value?.type === 'sentence') {
    wordContextSentence.value = bubble.value.sentence.en
  }
  bubble.value = { type: 'word', word, anchor }
}

function closeBubble() {
  bubble.value = null
}

function onDocClick(e: MouseEvent) {
  if (!bubble.value) return
  const target = e.target as HTMLElement
  // 点击气泡内部或句子块（会重新触发打开）不关闭
  if (target.closest('.bubble') || target.closest('.sentence')) return
  closeBubble()
}

function onKeydown(e: KeyboardEvent) {
  if (e.key === 'Escape') closeBubble()
}

// ---------- 朗读 ----------

function playOrResume() {
  if (ttsState.value === 'paused') {
    tts.resume(store.sentences.map((s) => s.en))
  } else if (ttsState.value === 'playing') {
    tts.pause()
  } else {
    tts.playSequence(0, store.sentences.map((s) => s.en))
  }
  ttsState.value = tts.getState()
}

function stopTts() {
  tts.stop()
  ttsState.value = tts.getState()
}

/** 朗读句滚动跟随（双侧） */
function scrollToSentence(index: number) {
  const target = `[data-seq="${index}"]`
  enScroller.value?.querySelector(target)?.scrollIntoView({ block: 'center', behavior: 'auto' })
  zhScroller.value?.querySelector(target)?.scrollIntoView({ block: 'center', behavior: 'auto' })
}

watch(ttsRate, (rate) => tts.setRate(rate))

// ---------- 加载 ----------

async function load() {
  const id = Number(route.params.id)
  if (!id) {
    errorMsg.value = '文章不存在'
    loading.value = false
    return
  }
  loading.value = true
  errorMsg.value = ''
  try {
    const payload = await articleApi.reading(id)
    article.value = payload.article
    store.load(id, payload.sentences, payload.progress.progress, payload.progress.isCompleted)
    store.setLearningData(payload.vocabWords, payload.favSentenceIds)
    tracking.init(payload.progress.readSentences)
    await scrollSync.measure()
    if (isDesktop.value) {
      scrollSync.setEnabled(true)
      tracking.observe(enScroller.value)
    } else {
      tracking.observe(null)
    }
    tracking.start()
  } catch {
    errorMsg.value = '文章加载失败，可能已下架或不存在'
  } finally {
    loading.value = false
  }
}

function onMqlChange(e: MediaQueryListEvent) {
  isDesktop.value = e.matches
  scrollSync.setEnabled(e.matches)
  scrollSync.measure()
  if (e.matches) {
    tracking.observe(enScroller.value)
  } else {
    tracking.observe(null)
  }
}

onMounted(() => {
  mql.addEventListener('change', onMqlChange)
  document.addEventListener('click', onDocClick)
  document.addEventListener('keydown', onKeydown)
  // TTS 进度回调：高亮 + 滚动跟随
  tts.onProgress = (index) => {
    store.setPlaying(index >= 0 ? index : null)
    if (index >= 0) scrollToSentence(index)
    ttsState.value = tts.getState()
  }
  load()
})

onBeforeUnmount(() => {
  mql.removeEventListener('change', onMqlChange)
  document.removeEventListener('click', onDocClick)
  document.removeEventListener('keydown', onKeydown)
  tts.stop()
  tts.onProgress = null
  tracking.flush()
  scrollSync.dispose()
  store.reset()
})

// 路由参数变化（同页切换文章）重新加载
watch(() => route.params.id, load)
</script>

<style scoped>
.reading-loading,
.reading-error {
  position: fixed;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: var(--space-3);
  background: var(--bg);
  color: var(--ink-2);
  z-index: 5;
}

.back-link {
  color: var(--accent);
  border-bottom: 1px solid var(--accent);
}

.tts-controls {
  display: flex;
  align-items: center;
  gap: var(--space-2);
}

.tts-btn {
  width: 32px;
  height: 32px;
  padding: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.9rem;
}

.rate-slider {
  width: 80px;
  accent-color: var(--accent);
}

.rate-label {
  font-size: 0.75rem;
  color: var(--ink-2);
  width: 28px;
  font-variant-numeric: tabular-nums;
}

@media (max-width: 1023px) {
  .rate-slider {
    display: none;
  }

  .rate-label {
    display: none;
  }
}
</style>
