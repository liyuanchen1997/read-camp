import { http } from './request'
import type { PageResult } from '@/types/api'
import type { ArticleDto, SentenceDto } from './article'

export interface ArticleForm {
  title: string
  summary?: string
  content: string
  tags?: string[]
  difficulty: 1 | 2 | 3
}

export interface GenStatus {
  total: number
  pending: number
  generating: number
  done: number
  failed: number
  running: boolean
  perSentence: Array<{
    sentenceId: number
    seq: number
    genStatus: number
    genError: string | null
  }>
}

export interface AdminStats {
  users: number
  articles: number
  published: number
  sentences: number
  genDone: number
  genFailed: number
}

export interface ArticleDetail {
  id: number
  title: string
  summary: string
  contentEn: string
  tags: string[] | null
  difficulty: number
  status: number
}

export interface AiConfig {
  baseUrl: string
  apiKey: string
  model: string
  batchSize: number
  temperature: number
  timeoutSeconds: number
}

export const adminApi = {
  // 文章管理
  list(params: { status?: number; keyword?: string; page?: number; size?: number }) {
    return http<PageResult<ArticleDto>>({ url: '/admin/articles', method: 'get', params })
  },
  create(form: ArticleForm) {
    return http<ArticleDto>({ url: '/admin/articles', method: 'post', data: form })
  },
  update(id: number, form: ArticleForm) {
    return http<ArticleDto>({ url: `/admin/articles/${id}`, method: 'put', data: form })
  },
  remove(id: number) {
    return http<void>({ url: `/admin/articles/${id}`, method: 'delete' })
  },
  changeStatus(id: number, status: 0 | 1) {
    return http<ArticleDto>({ url: `/admin/articles/${id}/status`, method: 'post', data: { status } })
  },

  /** 管理端详情（含正文，编辑回显） */
  detail(id: number) {
    return http<ArticleDetail>({ url: `/admin/articles/${id}`, method: 'get' })
  },

  // AI 生成
  generate(id: number, target: 'missing' | 'all', batchSize?: number) {
    return http<{ total: number }>({
      url: `/admin/articles/${id}/generate`,
      method: 'post',
      data: { target, batchSize },
    })
  },
  genStatus(id: number) {
    return http<GenStatus>({ url: `/admin/articles/${id}/gen-status`, method: 'get' })
  },
  generateOne(id: number, sentenceId: number) {
    return http<void>({
      url: `/admin/articles/${id}/sentences/${sentenceId}/generate`,
      method: 'post',
    })
  },
  cancel(id: number) {
    return http<boolean>({ url: `/admin/articles/${id}/generate/cancel`, method: 'post' })
  },

  // 仪表盘
  stats() {
    return http<AdminStats>({ url: '/admin/stats', method: 'get' })
  },

  // AI 模型配置
  getAiConfig() {
    return http<AiConfig>({ url: '/admin/ai-config', method: 'get' })
  },
  updateAiConfig(config: AiConfig) {
    return http<AiConfig>({ url: '/admin/ai-config', method: 'put', data: config })
  },
  testAiConfig(config: AiConfig) {
    return http<{ ok: boolean; reply: string }>({
      url: '/admin/ai-config/test',
      method: 'post',
      data: config,
    })
  },
}

export type { SentenceDto }
