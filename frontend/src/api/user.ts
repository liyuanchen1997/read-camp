import { http } from './request'
import type { UserDto } from '@/types/api'

export interface RecentReadingItem {
  articleId: number
  title: string
  coverUrl: string
  difficulty: number
  progress: number
  isCompleted: boolean
  lastReadAt: string
  completedAt: string | null
}

export const userApi = {
  /** 当前用户资料（含聚合统计） */
  me() {
    return http<UserDto>({ url: '/users/me', method: 'get' })
  },
  /** 更新昵称/头像 */
  updateProfile(params: { nickname?: string; avatarUrl?: string }) {
    return http<UserDto>({ url: '/users/me', method: 'put', data: params })
  },
  /** 近期阅读 */
  recentReading() {
    return http<RecentReadingItem[]>({ url: '/users/me/recent-reading', method: 'get' })
  },
}
