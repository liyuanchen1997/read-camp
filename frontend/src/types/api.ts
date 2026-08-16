/**
 * 与后端 DTO 对齐的类型定义（doc/00-design.md §2）
 * 后续步骤按需补充各模块类型
 */

export interface UserDto {
  id: number
  username: string
  nickname: string
  avatarUrl: string
  role: 0 | 1
  mustChangePassword: boolean
  /** 精读完成文章数 */
  completedCount?: number
  /** 进行中文章数 */
  readingCount?: number
  /** 平均阅读进度 0-100 */
  totalProgress?: number
}

export interface LoginResult {
  token: string
  user: UserDto
}

export interface PageResult<T> {
  records: T[]
  total: number
  page: number
  size: number
}
