import { http } from './request'
import type { PageResult } from '@/types/api'

export interface VocabItem {
  id: number
  word: string
  sourceArticleId: number | null
  contextSentence: string | null
  createdAt: string
}

export const vocabApi = {
  list(params: { keyword?: string; page?: number; size?: number }) {
    return http<PageResult<VocabItem>>({ url: '/vocab', method: 'get', params })
  },
  add(params: { word: string; sourceArticleId?: number | null; contextSentence?: string | null }) {
    return http<void>({ url: '/vocab', method: 'post', data: params })
  },
  remove(word: string) {
    return http<void>({ url: `/vocab/${encodeURIComponent(word)}`, method: 'delete' })
  },
}
