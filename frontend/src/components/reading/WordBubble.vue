<template>
  <Teleport to="body">
    <div
      ref="bubbleEl"
      class="bubble word-bubble"
      :style="style"
      @mouseenter="emit('enter')"
      @mouseleave="emit('leave')"
    >
      <div class="wb-head">
        <span class="wb-word">{{ word.word }}</span>
        <span v-if="word.pos" class="wb-pos">{{ word.pos }}</span>
      </div>

      <p v-if="word.meaning" class="wb-meaning">{{ word.meaning }}</p>
      <p v-if="word.role" class="wb-role">{{ word.role }}</p>

      <div class="wb-actions">
        <button class="action-btn" @click="speakWord">🔊 发音</button>
        <button class="action-btn" :class="{ active: inVocab }" @click="toggleVocab">
          {{ inVocab ? '✓ 已在生词本' : '＋ 加入生词本' }}
        </button>
      </div>
    </div>
  </Teleport>
</template>

<script setup lang="ts">
import { computed, nextTick, ref, watch } from 'vue'
import type { CSSProperties } from 'vue'
import { vocabApi } from '@/api/vocab'
import { useReadingStore } from '@/stores/reading'
import { tts } from '@/services/tts'
import { bubbleStyle } from '@/utils/bubble'

const props = defineProps<{
  word: { word: string; pos?: string; meaning?: string; role?: string }
  anchor: DOMRect
  sourceArticleId?: number | null
  contextSentence?: string
}>()

const emit = defineEmits<{
  enter: []
  leave: []
}>()

const store = useReadingStore()
const bubbleEl = ref<HTMLElement | null>(null)
const style = ref<CSSProperties>({ position: 'fixed', left: '-9999px' })

const inVocab = computed(() => store.hasVocab(props.word.word))

watch(
  () => props.anchor,
  async () => {
    await nextTick()
    // 单词浮层：紧凑宽度（280px），内容自适应
    style.value = bubbleStyle(props.anchor, bubbleEl.value, 280)
  },
  { immediate: true },
)

function speakWord() {
  tts.speak(props.word.word)
}

async function toggleVocab() {
  const added = !inVocab.value
  store.setVocab(props.word.word, added) // 乐观更新
  try {
    if (added) {
      await vocabApi.add({
        word: props.word.word,
        sourceArticleId: props.sourceArticleId,
        contextSentence: props.contextSentence,
      })
    } else {
      await vocabApi.remove(props.word.word)
    }
  } catch {
    store.setVocab(props.word.word, !added) // 失败回滚
  }
}
</script>

<style scoped>
.bubble {
  background: var(--bg-card);
  border: 1px solid var(--line);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow);
  padding: var(--space-3);
  font-size: 0.9rem;
}

.wb-head {
  display: flex;
  align-items: baseline;
  gap: var(--space-2);
}

.wb-word {
  font-family: var(--font-serif-en);
  font-size: 1.3rem;
  font-weight: 700;
  color: var(--accent);
}

.wb-pos {
  font-size: 0.8rem;
  color: var(--ink-3);
  font-style: italic;
}

.close-btn {
  margin-left: auto;
  border: none;
  background: transparent;
  color: var(--ink-3);
  cursor: pointer;
  font-size: 0.85rem;
}

.close-btn:hover {
  color: var(--ink);
}

.wb-meaning {
  color: var(--ink);
  font-size: 1rem;
  margin: var(--space-2) 0;
}

.wb-role {
  color: var(--ink-2);
  font-size: 0.85rem;
  line-height: 1.7;
  margin-bottom: var(--space-3);
}

.wb-actions {
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

.action-btn.active {
  border-color: var(--green);
  color: var(--green);
}
</style>
