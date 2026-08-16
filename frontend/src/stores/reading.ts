import { ref } from 'vue'
import { defineStore } from 'pinia'
import type { SentenceDto } from '@/api/article'

/**
 * 阅读页共享状态（单文章会话级）
 * hoveredIndex：单一数据源，双向高亮由两栏索引相等派生
 */
export const useReadingStore = defineStore('reading', () => {
  const articleId = ref<number | null>(null)
  const sentences = ref<SentenceDto[]>([])
  /** 悬停/激活的句子索引（null=无） */
  const hoveredIndex = ref<number | null>(null)
  /** 当前朗读句索引（null=未朗读） */
  const playingIndex = ref<number | null>(null)
  /** 当前文章进度 0-100 */
  const progress = ref(0)
  const isCompleted = ref(false)
  /** 我的生词集合（气泡"已加入"状态） */
  const vocabWords = ref<string[]>([])
  /** 我收藏的句子 id 集合（气泡心形状态） */
  const favSentenceIds = ref<number[]>([])

  function load(id: number, list: SentenceDto[], p: number, completed: boolean) {
    articleId.value = id
    sentences.value = list
    progress.value = p
    isCompleted.value = completed
    hoveredIndex.value = null
  }

  function setLearningData(vocab: string[], favs: number[]) {
    vocabWords.value = vocab
    favSentenceIds.value = favs
  }

  function setHover(index: number | null) {
    hoveredIndex.value = index
  }

  function setProgress(p: number, completed: boolean) {
    progress.value = p
    if (completed) isCompleted.value = true
  }

  function setPlaying(index: number | null) {
    playingIndex.value = index
  }

  function isFav(sentenceId: number) {
    return favSentenceIds.value.includes(sentenceId)
  }

  function setFav(sentenceId: number, fav: boolean) {
    if (fav) {
      if (!favSentenceIds.value.includes(sentenceId)) favSentenceIds.value.push(sentenceId)
    } else {
      favSentenceIds.value = favSentenceIds.value.filter((id) => id !== sentenceId)
    }
  }

  function hasVocab(word: string) {
    return vocabWords.value.includes(word)
  }

  function setVocab(word: string, added: boolean) {
    if (added) {
      if (!vocabWords.value.includes(word)) vocabWords.value.push(word)
    } else {
      vocabWords.value = vocabWords.value.filter((w) => w !== word)
    }
  }

  function reset() {
    articleId.value = null
    sentences.value = []
    hoveredIndex.value = null
    playingIndex.value = null
    progress.value = 0
    isCompleted.value = false
    vocabWords.value = []
    favSentenceIds.value = []
  }

  return {
    articleId, sentences, hoveredIndex, playingIndex, progress, isCompleted,
    vocabWords, favSentenceIds,
    load, setLearningData, setHover, setProgress, setPlaying,
    isFav, setFav, hasVocab, setVocab, reset,
  }
})
