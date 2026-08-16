import { ref } from 'vue'
import { defineStore } from 'pinia'

export const TOKEN_KEY = 'readcamp-token'

export interface UserInfo {
  id: number
  username: string
  nickname: string
  avatarUrl: string
  role: 0 | 1
  mustChangePassword: boolean
}

export const useUserStore = defineStore('user', () => {
  const token = ref<string>(localStorage.getItem(TOKEN_KEY) || '')
  const userInfo = ref<UserInfo | null>(null)

  function setToken(value: string) {
    token.value = value
    if (value) localStorage.setItem(TOKEN_KEY, value)
    else localStorage.removeItem(TOKEN_KEY)
  }

  function setUser(info: UserInfo | null) {
    userInfo.value = info
  }

  function logout() {
    setToken('')
    setUser(null)
  }

  const isLoggedIn = () => !!token.value
  const isAdmin = () => userInfo.value?.role === 1

  return { token, userInfo, setToken, setUser, logout, isLoggedIn, isAdmin }
})
