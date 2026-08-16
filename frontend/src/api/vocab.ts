import { http } from './request'
import type { PageResult } from '@/types/api'

export interface VocabItem {
  id: number
  word: string
  sourceArticleId: number | null
  contextSentence: string | null
  /** 词性（来自出处句 AI 标注） */
  pos?: string | null
  /** 中文意思 */
  meaning?: string | null
  /** 在句中的作用 */
  role?: string | null
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
