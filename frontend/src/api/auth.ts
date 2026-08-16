import { http } from './request'
import type { UserDto } from '@/types/api'

export interface LoginResult {
  token: string
  user: UserDto
}

export interface RegisterParams {
  username: string
  password: string
  nickname?: string
}

export const authApi = {
  /** 注册 */
  register(params: RegisterParams) {
    return http<UserDto>({ url: '/auth/register', method: 'post', data: params })
  },
  /** 登录 */
  login(params: { username: string; password: string }) {
    return http<LoginResult>({ url: '/auth/login', method: 'post', data: params })
  },
  /** 修改密码（首登强制改密后清除标记） */
  changePassword(params: { oldPassword: string; newPassword: string }) {
    return http<void>({ url: '/auth/password', method: 'put', data: params })
  },
}
