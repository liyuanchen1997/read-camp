import { ref } from 'vue'
import { defineStore } from 'pinia'

export type Theme = 'light' | 'dark'
const STORAGE_KEY = 'readcamp-theme'

export const useThemeStore = defineStore('theme', () => {
  const theme = ref<Theme>(
    (document.documentElement.dataset.theme as Theme) || 'light',
  )

  function apply(value: Theme) {
    theme.value = value
    document.documentElement.dataset.theme = value
  }

  function set(value: Theme) {
    apply(value)
    localStorage.setItem(STORAGE_KEY, value)
  }

  /** 跟随系统（清除手动选择） */
  function followSystem() {
    localStorage.removeItem(STORAGE_KEY)
    apply(
      window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light',
    )
  }

  function toggle() {
    set(theme.value === 'light' ? 'dark' : 'light')
  }

  return { theme, set, toggle, followSystem }
})
