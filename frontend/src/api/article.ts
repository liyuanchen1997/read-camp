import { http } from './request'
import type { PageResult } from '@/types/api'

export interface ArticleDto {
  id: number
  title: string
  summary: string
  coverUrl: string
  tags: string[] | null
  difficulty: 1 | 2 | 3
  status: number
  wordCount: number
  sentenceCount: number
  createdAt: string
}

export interface SentenceDto {
  id: number
  /** 章节 id（NULL=无章节旧数据，归入单章） */
  chapterId: number | null
  seq: number
  /** 章内段落号（段落流式排版） */
  para: number
  en: string
  zh: string | null
  explanation: string | null
  components: Array<{ type: string; text: string; detail?: string }> | null
  words: Array<{ word: string; pos?: string; meaning?: string; role?: string; phonetic?: string }> | null
  genStatus: number
}

export interface ProgressView {
  readSentences: number[]
  progress: number
  isCompleted: boolean
}

export interface ChapterDto {
  /** 章节 id（旧文章合成单章时为 null） */
  id: number | null
  seq: number
  title: string
}

export interface ReadingPayload {
  article: ArticleDto
  /** 章节列表（恒非空） */
  chapters: ChapterDto[]
  sentences: SentenceDto[]
  progress: ProgressView
  vocabWords: string[]
  favSentenceIds: number[]
}

export interface ShelfParams {
  keyword?: string
  difficulty?: number
  tag?: string
  page?: number
  size?: number
}

export const articleApi = {
  /** 书架分页（公开） */
  shelf(params: ShelfParams) {
    return http<PageResult<ArticleDto>>({ url: '/articles', method: 'get', params })
  },
  /** 文章详情（公开） */
  detail(id: number) {
    return http<ArticleDto>({ url: `/articles/${id}`, method: 'get' })
  },
  /** 阅读载荷（一次拉全） */
  reading(id: number) {
    return http<ReadingPayload>({ url: `/articles/${id}/reading`, method: 'get' })
  },
  /** 进度上报 */
  reportProgress(id: number, readSentenceIndexes: number[]) {
    return http<{ progress: number; isCompleted: boolean }>({
      url: `/articles/${id}/progress`,
      method: 'post',
      data: { readSentenceIndexes },
    })
  },
}
