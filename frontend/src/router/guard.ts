import type { RouteLocationNormalized } from 'vue-router'
import { useUserStore } from '@/stores/user'

/**
 * 全局守卫：登录态 + 管理员角色校验
 * - requiresAuth：未登录跳 /login（带 redirect 回跳）
 * - requiresAdmin：非管理员跳书架
 */
export async function authGuard(to: RouteLocationNormalized) {
  const userStore = useUserStore()

  if (!to.meta.requiresAuth) {
    return true
  }
  if (!userStore.isLoggedIn()) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  // 已登录但内存无用户信息（刷新场景）→ 拉取；失败则清理并跳登录
  if (!userStore.userInfo) {
    try {
      await userStore.fetchMe()
    } catch {
      userStore.logout()
      return { path: '/login', query: { redirect: to.fullPath } }
    }
  }
  if (to.meta.requiresAdmin && !userStore.isAdmin()) {
    return { path: '/' }
  }
  return true
}
