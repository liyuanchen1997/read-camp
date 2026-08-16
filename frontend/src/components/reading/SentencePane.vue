<template>
  <div ref="scrollerEl" class="pane-scroller" :class="side">
    <!-- 按章节分组渲染：章标题（双栏对称，data-chapter 供目录定位/高亮）→ 章内按段落分组 -->
    <template v-for="group in chapterGroups" :key="group.id ?? 'legacy'">
      <h2 class="chapter-title" :data-chapter="group.id ?? 'legacy'">
        {{ group.title }}
      </h2>
      <div
        v-for="pg in paraGroupsOf(group)"
        :key="`${group.id ?? 'legacy'}:${pg.para}`"
        class="para"
      >
        <SentenceBlock
          v-for="s in pg.sentences"
          :key="s.id"
          :sentence="s"
          :side="side"
          @click="(sentence, e) => emit('sentence-click', sentence, e)"
        />
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import type { SentenceDto } from '@/api/article'
import type { ChapterGroup } from '@/stores/reading'
import SentenceBlock from './SentenceBlock.vue'

const props = defineProps<{
  chapterGroups: ChapterGroup[]
  side: 'en' | 'zh'
}>()

const emit = defineEmits<{
  'sentence-click': [sentence: SentenceDto, event: MouseEvent]
}>()

/** 滚动容器元素（同步滚动/进度观察 root 用） */
const scrollerEl = ref<HTMLElement | null>(null)

/**
 * 章内段落分组：key 必须用 (chapterId, para) 二元组——
 * 章内 para 0 起，跨章会碰撞（第一段全为 para 0）。
 */
function paraGroupsOf(group: ChapterGroup) {
  const groups: { para: number; sentences: SentenceDto[] }[] = []
  for (const s of group.sentences) {
    const para = s.para ?? 0
    const last = groups[groups.length - 1]
    if (!last || last.para !== para) {
      groups.push({ para, sentences: [s] })
    } else {
      last.sentences.push(s)
    }
  }
  return groups
}

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

/* 章节标题：衬线，两栏对称同渲染（同步滚动锚点依赖 DOM 结构对称）；
   scroll-margin-top 避开工具栏（目录跳转锚点） */
.chapter-title {
  font-family: var(--font-serif-zh);
  font-size: 1.35rem;
  font-weight: 700;
  color: var(--ink);
  margin: 0 0 var(--space-4);
  padding-bottom: var(--space-2);
  border-bottom: 1px solid var(--line);
  scroll-margin-top: 64px;
}

/* 段落间距：组间空行（.sentence 为 inline，需靠 .para 的外边距分隔段落） */
.para {
  margin-bottom: 1.5em;
}

.para:last-child {
  margin-bottom: 0;
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
