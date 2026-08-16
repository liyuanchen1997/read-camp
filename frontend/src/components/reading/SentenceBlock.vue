<template>
  <div
    class="sentence"
    :class="[sideClass, { synced: hovered, playing: playing }]"
    :data-seq="sentence.seq"
    @mouseenter="onEnter"
    @mouseleave="onLeave"
    @click="onClick"
  >
    <template v-if="side === 'en'">{{ sentence.en }}</template>
    <template v-else>
      <span v-if="sentence.zh">{{ sentence.zh }}</span>
      <span v-else class="placeholder">标注未生成</span>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { SentenceDto } from '@/api/article'
import { useReadingStore } from '@/stores/reading'

const props = defineProps<{
  sentence: SentenceDto
  side: 'en' | 'zh'
}>()

const emit = defineEmits<{
  click: [sentence: SentenceDto, event: MouseEvent]
}>()

const store = useReadingStore()

const sideClass = computed(() => (props.side === 'zh' ? 'zh' : 'en'))
/** 双向高亮：两栏同索引同步（纯状态派生） */
const hovered = computed(() => store.hoveredIndex === props.sentence.seq)
const playing = computed(() => store.playingIndex === props.sentence.seq)

function onEnter() {
  store.setHover(props.sentence.seq)
}

function onLeave() {
  store.setHover(null)
}

function onClick(e: MouseEvent) {
  emit('click', props.sentence, e)
}
</script>
