import { http } from './request'
import type { PageResult } from '@/types/api'

export interface FavoriteItem {
  sentenceId: number
  en: string
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
