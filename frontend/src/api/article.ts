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
  seq: number
  en: string
  zh: string | null
  explanation: string | null
  components: Array<{ type: string; text: string; detail?: string }> | null
  words: Array<{ word: string; pos?: string; meaning?: string; role?: string }> | null
  genStatus: number
}

export interface ProgressView {
  readSentences: number[]
  progress: number
  isCompleted: boolean
}

export interface ReadingPayload {
  article: ArticleDto
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
