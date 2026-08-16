<template>
  <div class="reading-page">
    <ArticleToolbar
      :article="article"
      :progress="store.progress"
      :show-zh="isMobile ? mobileTab === 'zh' : showZh"
      :is-mobile="isMobile"
      @toggle-zh="showZh = !showZh"
    >
      <template #actions>
        <!-- 移动端：英文/中文 Tab + 对照模式 + 目录 -->
        <div v-if="isMobile" class="mobile-tabs">
          <button
            class="tab-btn"
            :class="{ active: mobileTab === 'en' }"
            @click="switchMobileTab('en')"
          >
            英文
          </button>
          <button
            class="tab-btn"
            :class="{ active: mobileTab === 'zh' }"
            @click="switchMobileTab('zh')"
          >
            中文
          </button>
          <button
            class="tab-btn mode"
            :class="{ active: mobileMode === 'dual' }"
            :title="mobileMode === 'dual' ? '切换为 Tab 模式' : '切换为上下对照'"
            @click="toggleMobileMode"
          >
            ⇅
          </button>
          <button
            class="tab-btn mode"
            :class="{ active: tocOpen }"
            title="目录"
            @click="tocOpen = !tocOpen"
          >
            ☰
          </button>
          <!-- 折叠目录下拉（移动端） -->
          <ChapterToc
            v-if="tocOpen"
            :groups="store.chapterGroups"
            :active-id="activeChapter"
            variant="dropdown"
            @select="(a) => { tocOpen = false; scrollToChapter(a) }"
            @prev="goAdjacent(-1)"
            @next="goAdjacent(1)"
          />
        </div>
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

    <!-- 桌面：目录列 + 双栏 -->
    <div v-if="!isMobile" class="reading-layout" :class="{ 'show-zh': showZh }">
      <aside class="toc-col">
        <ChapterToc
          :groups="store.chapterGroups"
          :active-id="activeChapter"
          variant="aside"
          @select="scrollToChapter"
          @prev="goAdjacent(-1)"
          @next="goAdjacent(1)"
        />
      </aside>
      <SentencePane
        ref="enPaneRef"
        side="en"
        :chapter-groups="store.chapterGroups"
        @sentence-click="onSentenceClick"
      />
      <SentencePane
        v-show="showZh"
        ref="zhPaneRef"
        side="zh"
        :chapter-groups="store.chapterGroups"
        @sentence-click="onSentenceClick"
      />
    </div>

    <!-- 移动端 Tab 模式：单 pane -->
    <div v-else-if="mobileMode === 'tab'" class="reading-layout mobile">
      <SentencePane
        v-if="mobileTab === 'en'"
        ref="enPaneRef"
        side="en"
        :chapter-groups="store.chapterGroups"
        @sentence-click="onSentenceClick"
      />
      <SentencePane
        v-else
        ref="zhPaneRef"
        side="zh"
        :chapter-groups="store.chapterGroups"
        @sentence-click="onSentenceClick"
      />
    </div>

    <!-- 移动端上下对照 -->
    <div v-else class="reading-layout mobile-dual">
      <SentencePane
        ref="enPaneRef"
        side="en"
        :chapter-groups="store.chapterGroups"
        @sentence-click="onSentenceClick"
      />
      <SentencePane
        ref="zhPaneRef"
        side="zh"
        :chapter-groups="store.chapterGroups"
        @sentence-click="onSentenceClick"
      />
    </div>

    <!-- 句子气泡（单词解释为气泡内 hover 浮层） -->
    <SentenceBubble
      v-if="bubble?.type === 'sentence'"
      :sentence="bubble.sentence"
      :anchor="bubble.anchor"
      :source-article-id="store.articleId"
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
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { articleApi, type ArticleDto, type SentenceDto } from '@/api/article'
import ArticleToolbar from '@/components/reading/ArticleToolbar.vue'
import ChapterToc from '@/components/reading/ChapterToc.vue'
import SentenceBubble from '@/components/reading/SentenceBubble.vue'
import SentencePane from '@/components/reading/SentencePane.vue'
import { useReadTracking } from '@/composables/useReadTracking'
import { useScrollSync } from '@/composables/useScrollSync'
import { tts } from '@/services/tts'
import { useReadingStore } from '@/stores/reading'
import '@/styles/reading.css'

type BubbleState = { type: 'sentence'; sentence: SentenceDto; anchor: DOMRect } | null

const route = useRoute()
const store = useReadingStore()

const article = ref<ArticleDto | null>(null)
const showZh = ref(false)
const loading = ref(true)
const errorMsg = ref('')
const bubble = ref<BubbleState>(null)

const ttsState = ref(tts.getState())
const ttsRate = ref(tts.getRate())

const enPaneRef = ref<InstanceType<typeof SentencePane> | null>(null)
const zhPaneRef = ref<InstanceType<typeof SentencePane> | null>(null)

const enScroller = computed(() => enPaneRef.value?.scrollerEl ?? null)
const zhScroller = computed(() => zhPaneRef.value?.scrollerEl ?? null)

/** 桌面双栏模式（≥1024px）才启用同步滚动与双栏观察 */
const isDesktop = ref(window.matchMedia('(min-width: 1024px)').matches)
const mql = window.matchMedia('(min-width: 1024px)')

/** 移动端模式：tab=英文/中文切换（默认）；dual=上下对照 */
const isMobile = computed(() => !isDesktop.value)
const mobileTab = ref<'en' | 'zh'>('en')
const mobileMode = ref<'tab' | 'dual'>('tab')

// ---------- 目录 ----------

/** 当前章锚点（'legacy'=无章节旧文章合成章） */
const activeChapter = ref<number | null>(null)
/** 移动端目录下拉开关 */
const tocOpen = ref(false)
let chapterObserver: IntersectionObserver | null = null

/** 章节标题高亮观察：root=当前挂载的滚动容器，进入视口顶部 25% 区的章置为当前 */
function observeChapters() {
  chapterObserver?.disconnect()
  chapterObserver = null
  const root = isDesktop.value ? enScroller.value : enScroller.value ?? zhScroller.value
  if (!root || typeof IntersectionObserver === 'undefined') return
  chapterObserver = new IntersectionObserver(
    (entries) => {
      for (const e of entries) {
        if (e.isIntersecting) {
          const anchor = (e.target as HTMLElement).dataset.chapter ?? 'legacy'
          activeChapter.value = anchor === 'legacy' ? null : Number(anchor)
        }
      }
    },
    { root, rootMargin: '0px 0px -75% 0px', threshold: 0 },
  )
  root.querySelectorAll('[data-chapter]').forEach((el) => chapterObserver!.observe(el))
}

/** 滚动到指定章（[data-chapter] 锚点；瞬时跳转与 scrollSync 互斥锁语义一致，避免 smooth 回弹） */
function scrollToChapter(anchor: string) {
  const sel = `[data-chapter="${anchor}"]`
  enScroller.value?.querySelector(sel)?.scrollIntoView({ block: 'start', behavior: 'auto' })
  zhScroller.value?.querySelector(sel)?.scrollIntoView({ block: 'start', behavior: 'auto' })
}

/** 上/下一章（相对当前高亮章） */
function goAdjacent(dir: -1 | 1) {
  const groups = store.chapterGroups
  if (!groups.length) return
  const idx = groups.findIndex((g) => (g.id ?? 'legacy') === (activeChapter.value ?? 'legacy'))
  const next = idx === -1 ? (dir > 0 ? 0 : groups.length - 1) : idx + dir
  if (next < 0 || next >= groups.length) return
  scrollToChapter(groups[next].id === null ? 'legacy' : String(groups[next].id))
}

/** 切换 Tab：重建 pane 后重新观察进度 */
function switchMobileTab(tab: 'en' | 'zh') {
  if (mobileTab.value === tab) return
  mobileTab.value = tab
  nextTick(reObserve)
}

function toggleMobileMode() {
  mobileMode.value = mobileMode.value === 'tab' ? 'dual' : 'tab'
  mobileTab.value = 'en'
  nextTick(reObserve)
}

/** 重建 IntersectionObserver（pane 切换后新句子需重新观察；章节高亮观察器同步重建） */
function reObserve() {
  if (isDesktop.value) {
    tracking.observe(enScroller.value)
  } else {
    tracking.observe(null)
  }
  observeChapters()
}

const scrollSync = useScrollSync(enScroller, zhScroller)
const tracking = useReadTracking(Number(route.params.id))

// ---------- 气泡 ----------

function onSentenceClick(sentence: SentenceDto, event: MouseEvent) {
  const el = (event.target as HTMLElement).closest('.sentence')
  const anchor = el?.getBoundingClientRect() ?? new DOMRect(0, 0, 0, 0)
  bubble.value = { type: 'sentence', sentence, anchor }
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
    store.load(
      id,
      payload.sentences,
      payload.chapters,
      payload.article.title,
      payload.progress.progress,
      payload.progress.isCompleted,
    )
    store.setLearningData(payload.vocabWords, payload.favSentenceIds)
    tracking.init(payload.progress.readSentences)
    await scrollSync.measure()
    if (isDesktop.value) {
      scrollSync.setEnabled(true)
      tracking.observe(enScroller.value)
    } else {
      tracking.observe(null)
    }
    observeChapters()
    tracking.start()
  } catch {
    errorMsg.value = '文章加载失败，可能已下架或不存在'
  } finally {
    loading.value = false
  }
}

function onMqlChange(e: MediaQueryListEvent) {
  isDesktop.value = e.matches
  tocOpen.value = false
  scrollSync.setEnabled(e.matches)
  scrollSync.measure()
  if (e.matches) {
    tracking.observe(enScroller.value)
  } else {
    tracking.observe(null)
  }
  observeChapters()
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
