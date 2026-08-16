<template>
  <div ref="scrollerEl" class="pane-scroller" :class="side">
    <!-- 按段落分组渲染：段内句子 inline 流式（视觉为正常文章），段间空行 -->
    <div v-for="(group, gi) in paraGroups" :key="gi" class="para">
      <SentenceBlock
        v-for="s in group"
        :key="s.id"
        :sentence="s"
        :side="side"
        @click="(sentence, e) => emit('sentence-click', sentence, e)"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import type { SentenceDto } from '@/api/article'
import SentenceBlock from './SentenceBlock.vue'

const props = defineProps<{
  sentences: SentenceDto[]
  side: 'en' | 'zh'
}>()

const emit = defineEmits<{
  'sentence-click': [sentence: SentenceDto, event: MouseEvent]
}>()

/** 滚动容器元素（同步滚动/进度观察 root 用） */
const scrollerEl = ref<HTMLElement | null>(null)

/** 按 para 分组（保持 seq 顺序） */
const paraGroups = computed(() => {
  const groups: SentenceDto[][] = []
  for (const s of props.sentences) {
    const para = s.para ?? 0
    if (!groups[para]) groups[para] = []
    groups[para].push(s)
  }
  return groups
})

defineExpose({ scrollerEl })
</script>

<style scoped>
.pane-scroller {
  min-width: 0;
  overflow-y: auto;
  overscroll-behavior: contain;
}

.pane-scroller.en {
  padding: var(--space-6) clamp(24px, 4vw, 64px) var(--space-7);
}

.pane-scroller.zh {
  border-left: 1px solid var(--line);
  padding: var(--space-6) clamp(24px, 4vw, 64px) var(--space-7);
}

@media (max-width: 1023px) {
  .pane-scroller {
    overflow: visible;
    padding: var(--space-5) var(--space-4) var(--space-7) !important;
  }

  .pane-scroller.zh {
    border-left: none;
    border-top: 1px solid var(--line);
  }
}
</style>
