import { ref } from 'vue'

/** 桌面视口（≥1024px）响应式判断：管理后台仅桌面端可用 */
const mql = window.matchMedia('(min-width: 1024px)')

export const isDesktop = ref(mql.matches)

mql.addEventListener('change', (e) => {
  isDesktop.value = e.matches
})
