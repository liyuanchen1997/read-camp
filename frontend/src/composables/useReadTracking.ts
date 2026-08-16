import { onBeforeUnmount, ref } from 'vue'
import { articleApi } from '@/api/article'
import { useReadingStore } from '@/stores/reading'

/**
 * 阅读进度跟踪（doc/00-design.md §4）
 * IntersectionObserver(threshold 0.2) 观察英文句子进入视口 → 标记已读；
 * 防抖批量上报：3s 定时 / 累计 30 条 / pagehide 时刷新。
 * root：桌面=英文栏滚动容器；移动端=null（视口）。
 */
export function useReadTracking(articleId: number) {
  const store = useReadingStore()

  /** 已读集合（从载荷初始化，避免重复上报；后端并集去重双保险） */
  const readSet = ref<Set<number>>(new Set())
  const pending = new Set<number>()
  let observer: IntersectionObserver | null = null
  let timer: number | null = null
  let flushing = false

  function init(initialRead: number[]) {
    readSet.value = new Set(initialRead)
  }

  function observe(root: HTMLElement | null) {
    observer = new IntersectionObserver(
      (entries) => {
        for (const entry of entries) {
          if (!entry.isIntersecting) continue
          const seq = Number((entry.target as HTMLElement).dataset.seq)
          if (Number.isNaN(seq) || readSet.value.has(seq)) continue
          readSet.value.add(seq)
          pending.add(seq)
          if (pending.size >= 30) flush()
        }
      },
      { root, threshold: 0.2 },
    )
    const container = root ?? document
    container.querySelectorAll<HTMLElement>('[data-seq]').forEach((el) => observer?.observe(el))
  }

  async function flush() {
    if (flushing || pending.size === 0) return
    flushing = true
    const indexes = [...pending]
    pending.clear()
    try {
      const result = await articleApi.reportProgress(articleId, indexes)
      store.setProgress(result.progress, result.isCompleted)
    } catch {
      // 上报失败：回填 pending 待下次重试
      indexes.forEach((i) => pending.add(i))
    } finally {
      flushing = false
    }
  }

  function start() {
    timer = window.setInterval(() => {
      if (pending.size > 0) flush()
    }, 3000)
    window.addEventListener('pagehide', flush)
  }

  function stop() {
    if (timer !== null) window.clearInterval(timer)
    window.removeEventListener('pagehide', flush)
    observer?.disconnect()
    observer = null
  }

  onBeforeUnmount(stop)

  return { init, observe, start, flush, readSet }
}
