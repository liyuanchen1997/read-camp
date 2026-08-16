import { ref } from 'vue'
import { defineStore } from 'pinia'
import { authApi } from '@/api/auth'
import { userApi } from '@/api/user'
import type { UserDto } from '@/types/api'

export const TOKEN_KEY = 'readcamp-token'

export const useUserStore = defineStore('user', () => {
  const token = ref<string>(localStorage.getItem(TOKEN_KEY) || '')
  const userInfo = ref<UserDto | null>(null)

  function setToken(value: string) {
    token.value = value
    if (value) localStorage.setItem(TOKEN_KEY, value)
    else localStorage.removeItem(TOKEN_KEY)
  }

  function setUser(info: UserDto | null) {
    userInfo.value = info
  }

  /** 登录：保存 token 与用户信息 */
  async function login(username: string, password: string) {
    const result = await authApi.login({ username, password })
    setToken(result.token)
    setUser(result.user)
    return result.user
  }

  /** 注册（不自动登录，返回用户信息） */
  function register(params: { username: string; password: string; nickname?: string }) {
    return authApi.register(params)
  }

  /** 拉取当前用户信息（刷新页面后恢复登录态） */
  async function fetchMe() {
    const me = await userApi.me()
    setUser(me)
    return me
  }

  function logout() {
    setToken('')
    setUser(null)
  }

  const isLoggedIn = () => !!token.value
  const isAdmin = () => userInfo.value?.role === 1

  return { token, userInfo, login, register, fetchMe, logout, setToken, setUser, isLoggedIn, isAdmin }
})
