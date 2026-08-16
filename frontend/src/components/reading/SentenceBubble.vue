<template>
  <Teleport to="body">
    <div ref="bubbleEl" class="bubble sentence-bubble" :style="style" role="dialog" @click="onBubbleClick">
      <div class="bubble-head">
        <span class="bubble-title">句子讲解</span>
        <button class="close-btn" title="关闭" @click="emit('close')">✕</button>
      </div>

      <!-- 原句 -->
      <p class="bubble-original">{{ sentence.en }}</p>

      <!-- 未生成提示 -->
      <p v-if="!sentence.zh && !sentence.explanation" class="bubble-empty">
        该句的精读标注尚未生成，请等待管理员生成
      </p>

      <template v-else>
        <p v-if="sentence.explanation" class="bubble-explanation">{{ sentence.explanation }}</p>
        <p v-if="sentence.zh" class="bubble-zh">{{ sentence.zh }}</p>

        <!-- 句子成分（成分类型 + 对应原文片段） -->
        <div v-if="sentence.components?.length" class="bubble-components">
          <span
            v-for="(c, i) in sentence.components"
            :key="i"
            class="comp-tag"
            :title="c.detail || c.text"
          >
            <span class="comp-type">{{ c.type }}</span>
            <span class="comp-text">{{ c.text }}</span>
          </span>
        </div>

        <!-- 单词列表：点击展示单词解释浮层（常驻可操作，再点关闭） -->
        <div v-if="sentence.words?.length" class="bubble-words">
          <button
            v-for="(w, i) in sentence.words"
            :key="i"
            class="word-item"
            @click="onWordClick(w, $event)"
          >
            <span class="w-word">{{ w.word }}</span>
            <span v-if="w.pos" class="w-pos">{{ w.pos }}</span>
            <span v-if="w.meaning" class="w-meaning">{{ w.meaning }}</span>
          </button>
        </div>
      </template>

      <!-- 操作栏 -->
      <div class="bubble-actions">
        <button
          class="action-btn"
          :class="{ fav: isFav }"
          @click="toggleFav"
        >
          {{ isFav ? '♥ 已收藏' : '♡ 收藏例句' }}
        </button>
        <button class="action-btn" @click="speakSentence">🔊 朗读本句</button>
      </div>
    </div>

    <!-- 单词解释浮层（点击触发，锚定单词项，常驻可操作） -->
    <WordBubble
      v-if="hoveredWord"
      :word="hoveredWord.word"
      :anchor="hoveredWord.anchor"
      :source-article-id="sourceArticleId"
      :context-sentence="sentence.en"
    />
  </Teleport>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { favoriteApi } from '@/api/favorite'
import type { SentenceDto } from '@/api/article'
import { useBubblePosition } from '@/composables/useBubblePosition'
import { useReadingStore } from '@/stores/reading'
import { tts } from '@/services/tts'
import WordBubble from './WordBubble.vue'

const props = defineProps<{
  sentence: SentenceDto
  anchor: DOMRect
  sourceArticleId?: number | null
}>()

const emit = defineEmits<{
  close: []
}>()

const store = useReadingStore()
const bubbleEl = ref<HTMLElement | null>(null)
const anchorRef = computed(() => props.anchor)

// 定位：挂载后重测 + ResizeObserver 持续校正（翻转/贴底/视口内完整可见）
const { style } = useBubblePosition(anchorRef, bubbleEl)

/** 点击选中的单词浮层状态（锚定单词项，常驻） */
const hoveredWord = ref<{
  word: { word: string; pos?: string; meaning?: string; role?: string }
  anchor: DOMRect
} | null>(null)

const isFav = computed(() => store.isFav(props.sentence.id))

/** 点击单词：展示浮层（再点同一单词关闭，点其他单词切换） */
function onWordClick(
  w: { word: string; pos?: string; meaning?: string; role?: string },
  event: MouseEvent,
) {
  if (hoveredWord.value?.word.word === w.word) {
    hoveredWord.value = null
    return
  }
  const el = (event.target as HTMLElement).closest('.word-item')
  const anchor = el?.getBoundingClientRect() ?? new DOMRect(0, 0, 0, 0)
  hoveredWord.value = { word: w, anchor }
}

/** 点击气泡内非单词区域：关闭浮层 */
function onBubbleClick(e: MouseEvent) {
  if ((e.target as HTMLElement).closest('.word-item')) return
  hoveredWord.value = null
}

async function toggleFav() {
  const fav = !isFav.value
  store.setFav(props.sentence.id, fav) // 乐观更新
  try {
    if (fav) await favoriteApi.add(props.sentence.id)
    else await favoriteApi.remove(props.sentence.id)
  } catch {
    store.setFav(props.sentence.id, !fav) // 失败回滚
  }
}

function speakSentence() {
  tts.speak(props.sentence.en)
}
</script>

<style scoped>
.bubble {
  background: var(--bg-card);
  border: 1px solid var(--line);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow);
  padding: var(--space-4);
  font-size: 0.9rem;
  /* 移动端视口矮：限制最大高度，超长时气泡内部滚动（定位翻转按实际高度计算）
     100dvh = 动态视口（移动端浏览器地址栏收起/展开时跟随），vh 作兜底 */
  max-height: calc(100vh - 24px);
  max-height: calc(100dvh - 24px);
  overflow-y: auto;
  overscroll-behavior: contain;
}

.bubble-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--space-2);
}

.bubble-title {
  font-size: 0.78rem;
  color: var(--ink-3);
  letter-spacing: 0.05em;
}

.close-btn {
  border: none;
  background: transparent;
  color: var(--ink-3);
  cursor: pointer;
  font-size: 0.85rem;
}

.close-btn:hover {
  color: var(--ink);
}

.bubble-original {
  font-family: var(--font-serif-en);
  font-size: 1.05rem;
  line-height: 1.7;
  color: var(--ink);
  margin-bottom: var(--space-3);
}

.bubble-empty {
  color: var(--ink-3);
  font-size: 0.85rem;
}

.bubble-explanation {
  color: var(--ink);
  font-size: 0.88rem;
  line-height: 1.7;
  margin-bottom: var(--space-2);
}

.bubble-zh {
  color: var(--ink-2);
  font-family: var(--font-serif-zh);
  font-size: 0.92rem;
  line-height: 1.8;
  margin-bottom: var(--space-3);
}

/* 成分标签 */
.bubble-components {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-2);
  margin-bottom: var(--space-3);
}

.comp-tag {
  display: inline-flex;
  align-items: baseline;
  gap: 4px;
  padding: 3px 10px;
  border-radius: 999px;
  background: var(--bg-hover);
  font-size: 0.75rem;
  cursor: help;
}

.comp-type {
  color: var(--accent);
  font-weight: 600;
}

.comp-text {
  font-family: var(--font-serif-en);
  color: var(--ink);
  font-style: italic;
  max-width: 180px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 单词列表 */
.bubble-words {
  max-height: 220px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 2px;
  margin-bottom: var(--space-3);
}

.word-item {
  display: flex;
  align-items: baseline;
  gap: var(--space-2);
  padding: 6px 8px;
  border: none;
  border-radius: 6px;
  background: transparent;
  color: var(--ink);
  cursor: pointer;
  text-align: left;
  transition: background var(--transition-fast);
}

.word-item:hover {
  background: var(--bg-hover);
}

.w-word {
  font-family: var(--font-serif-en);
  font-weight: 600;
  color: var(--accent);
}

.w-pos {
  font-size: 0.72rem;
  color: var(--ink-3);
  font-style: italic;
}

.w-meaning {
  font-size: 0.85rem;
  color: var(--ink-2);
}

/* 操作栏 */
.bubble-actions {
  display: flex;
  gap: var(--space-2);
  border-top: 1px solid var(--line);
  padding-top: var(--space-3);
}

.action-btn {
  padding: 5px 12px;
  border: 1px solid var(--line);
  border-radius: 999px;
  background: transparent;
  color: var(--ink-2);
  font-size: 0.8rem;
  cursor: pointer;
  transition: all var(--transition-fast);
}

.action-btn:hover {
  border-color: var(--accent);
  color: var(--accent);
}

.action-btn.fav {
  border-color: var(--green);
  color: var(--green);
}
</style>
