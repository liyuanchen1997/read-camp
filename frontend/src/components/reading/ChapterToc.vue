<template>
  <nav class="toc" :class="variant">
    <p v-if="variant === 'aside'" class="toc-label">目录</p>
    <ul class="toc-list">
      <li v-for="(g, i) in groups" :key="g.id ?? 'legacy'">
        <button
          class="toc-item"
          :class="{ active: activeId === (g.id ?? 'legacy') }"
          @click="emit('select', g.id === null ? 'legacy' : String(g.id))"
        >
          <span class="toc-index">{{ i + 1 }}</span>
          <span class="toc-title">{{ g.title }}</span>
        </button>
      </li>
    </ul>
    <div class="toc-nav">
      <button
        class="toc-prev"
        :disabled="!hasPrev"
        title="上一章"
        @click="emit('prev')"
      >↑ 上一章</button>
      <button
        class="toc-next"
        :disabled="!hasNext"
        title="下一章"
        @click="emit('next')"
      >下一章 ↓</button>
    </div>
  </nav>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { ChapterGroup } from '@/stores/reading'

const props = defineProps<{
  groups: ChapterGroup[]
  /** 当前章锚点（id ?? 'legacy'） */
  activeId: number | null
  variant: 'aside' | 'dropdown'
}>()

const emit = defineEmits<{
  select: [anchor: string]
  prev: []
  next: []
}>()

const activeIndex = computed(() =>
  props.groups.findIndex((g) => (g.id ?? 'legacy') === props.activeId),
)
const hasPrev = computed(() => activeIndex.value > 0)
const hasNext = computed(() => activeIndex.value >= 0 && activeIndex.value < props.groups.length - 1)
</script>

<style scoped>
.toc {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
}

.toc-label {
  font-size: 0.75rem;
  letter-spacing: 0.1em;
  color: var(--ink-3);
  padding: var(--space-4) var(--space-4) var(--space-2);
  text-transform: uppercase;
}

.toc-list {
  list-style: none;
  margin: 0;
  padding: 0;
  flex: 1;
  overflow-y: auto;
  min-height: 0;
}

.toc-item {
  display: flex;
  align-items: baseline;
  gap: var(--space-2);
  width: 100%;
  padding: 8px var(--space-4);
  border: none;
  background: transparent;
  color: var(--ink-2);
  font-size: 0.88rem;
  text-align: left;
  cursor: pointer;
  border-left: 3px solid transparent;
  transition: all var(--transition-fast);
}

.toc-item:hover {
  background: var(--bg-hover);
  color: var(--ink);
}

.toc-item.active {
  background: var(--bg-hover);
  color: var(--accent);
  border-left-color: var(--accent);
  font-weight: 600;
}

.toc-index {
  font-size: 0.72rem;
  color: var(--ink-3);
  font-variant-numeric: tabular-nums;
  flex-shrink: 0;
}

.toc-item.active .toc-index {
  color: var(--accent);
}

.toc-title {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 上一章/下一章 */
.toc-nav {
  display: flex;
  gap: var(--space-2);
  padding: var(--space-3) var(--space-4);
  border-top: 1px solid var(--line);
}

.toc-prev,
.toc-next {
  flex: 1;
  padding: 6px 0;
  border: 1px solid var(--line);
  border-radius: var(--radius-sm);
  background: transparent;
  color: var(--ink-2);
  font-size: 0.8rem;
  cursor: pointer;
  transition: all var(--transition-fast);
}

.toc-prev:hover:not(:disabled),
.toc-next:hover:not(:disabled) {
  border-color: var(--accent);
  color: var(--accent);
}

.toc-prev:disabled,
.toc-next:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

/* dropdown 变体：绝对定位下拉面板（移动端） */
.toc.dropdown {
  position: absolute;
  top: calc(100% + 8px);
  right: var(--space-3);
  width: min(280px, calc(100vw - 32px));
  max-height: 60vh;
  background: var(--bg-card);
  border: 1px solid var(--line);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow);
  z-index: 60;
}

.toc.dropdown .toc-label {
  padding-top: var(--space-3);
}
</style>
