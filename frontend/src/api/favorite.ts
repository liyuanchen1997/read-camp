import { http } from './request'
import type { PageResult } from '@/types/api'

export interface FavoriteItem {
  sentenceId: number
  en: string
  /** 中文翻译（AI 标注） */
  zh?: string | null
  /** 中文讲解（AI 标注） */
  explanation?: string | null
  /** 单词标注（AI 标注，含 phonetic） */
  words?: Array<{ word: string; pos?: string; meaning?: string; role?: string; phonetic?: string }> | null
  seq: number
  articleId: number
  articleTitle: string
  createdAt: string
}

export const favoriteApi = {
  list(params: { page?: number; size?: number }) {
    return http<PageResult<FavoriteItem>>({ url: '/favorites/sentences', method: 'get', params })
  },
  add(sentenceId: number) {
    return http<void>({ url: '/favorites/sentences', method: 'post', data: { sentenceId } })
  },
  remove(sentenceId: number) {
    return http<void>({ url: `/favorites/sentences/${sentenceId}`, method: 'delete' })
  },
}
