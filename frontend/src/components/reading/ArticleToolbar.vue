<template>
  <div class="reading-toolbar">
    <h1 class="toolbar-title" :title="article?.title ?? ''">{{ article?.title ?? '精读中…' }}</h1>

    <div class="toolbar-progress">
      <div class="track"><div class="fill" :style="{ width: progress + '%' }"></div></div>
      <span class="pct">{{ progress }}%</span>
    </div>

    <div class="toolbar-actions">
      <button
        class="toolbar-btn"
        :class="{ active: showZh }"
        @click="emit('toggle-zh')"
      >
        {{ showZh ? '隐藏翻译' : '显示翻译' }}
      </button>
      <!-- 朗读控制（步骤 9） -->
      <slot name="actions" />
    </div>
  </div>
</template>

<script setup lang="ts">
import type { ArticleDto } from '@/api/article'

defineProps<{
  article: ArticleDto | null
  progress: number
  showZh: boolean
}>()

const emit = defineEmits<{
  'toggle-zh': []
}>()
</script>
