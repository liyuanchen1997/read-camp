import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import type { ChapterDto, SentenceDto } from '@/api/article'

/** 章节分组（阅读页渲染/目录用）：按句子 chapterId 顺序分组，标题查 chapters ?? 文章标题 */
export interface ChapterGroup {
  id: number | null
  title: string
  firstSeq: number
  sentences: SentenceDto[]
}

/**
 * 阅读页共享状态（单文章会话级）
 * hoveredIndex：单一数据源，双向高亮由两栏索引相等派生
 */
export const useReadingStore = defineStore('reading', () => {
  const articleId = ref<number | null>(null)
  const sentences = ref<SentenceDto[]>([])
  const chapters = ref<ChapterDto[]>([])
  /** 文章标题（无章节回退标题用） */
  const articleTitle = ref('')
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

  /**
   * 章节分组：按 chapterId 顺序分组（保持 seq），title 从 chapters 查；
   * chapterId 为 null 的句子聚为一组，标题回退文章标题（存量旧数据兜底）。
   */
  const chapterGroups = computed<ChapterGroup[]>(() => {
    const groups: ChapterGroup[] = []
    let current: ChapterGroup | null = null
    for (const s of sentences.value) {
      const id = s.chapterId ?? null
      if (!current || current.id !== id) {
        current = {
          id,
          title: chapters.value.find((c) => c.id === id)?.title ?? articleTitle.value,
          firstSeq: s.seq,
          sentences: [],
        }
        groups.push(current)
      }
      current.sentences.push(s)
    }
    return groups
  })

  function load(
    id: number,
    list: SentenceDto[],
    chs: ChapterDto[],
    title: string,
    p: number,
    completed: boolean,
  ) {
    articleId.value = id
    sentences.value = list
    chapters.value = chs
    articleTitle.value = title
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
    chapters.value = []
    articleTitle.value = ''
    hoveredIndex.value = null
    playingIndex.value = null
    progress.value = 0
    isCompleted.value = false
    vocabWords.value = []
    favSentenceIds.value = []
  }

  return {
    articleId, sentences, chapters, articleTitle, chapterGroups,
    hoveredIndex, playingIndex, progress, isCompleted,
    vocabWords, favSentenceIds,
    load, setLearningData, setHover, setProgress, setPlaying,
    isFav, setFav, hasVocab, setVocab, reset,
  }
})
