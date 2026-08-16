<template>
  <div class="reading-toolbar">
    <h1 class="toolbar-title" :title="article?.title ?? ''">{{ article?.title ?? '精读中…' }}</h1>

    <div class="toolbar-progress">
      <div class="track"><div class="fill" :style="{ width: progress + '%' }"></div></div>
      <span class="pct">{{ progress }}%</span>
    </div>

    <div class="toolbar-actions">
      <!-- 桌面端：翻译开关（移动端由 Tab 切换替代） -->
      <button
        v-if="!isMobile"
        class="toolbar-btn"
        :class="{ active: showZh }"
        @click="emit('toggle-zh')"
      >
        {{ showZh ? '隐藏翻译' : '显示翻译' }}
      </button>
      <!-- 朗读控制 -->
      <slot name="actions" />
    </div>
  </div>
</template>

<script setup lang="ts">
import type { ArticleDto } from '@/api/article'

withDefaults(defineProps<{
  article: ArticleDto | null
  progress: number
  showZh: boolean
  isMobile?: boolean
}>(), {
  isMobile: false,
})

const emit = defineEmits<{
  'toggle-zh': []
}>()
</script>
