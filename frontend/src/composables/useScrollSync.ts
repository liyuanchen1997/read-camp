import { nextTick, type Ref } from 'vue'

/**
 * 双语双栏同步滚动（doc/00-design.md §4）
 * 机制：锚句 = 最接近视口顶部 25% 位置的句子；滚动源变化时把目标栏对应句子
 *       scrollIntoView(瞬时) 定位；互斥锁防止程序滚动触发反向同步（反馈环）。
 * 仅在桌面双栏模式启用（enabled 由外部 matchMedia 控制）。
 */

interface ScrollSync {
  /** 句子渲染完成后测量锚点（需在数据加载后调用） */
  measure: () => Promise<void>
  /** 启用/停用（绑定/解绑 scroll 监听） */
  setEnabled: (enabled: boolean) => void
  dispose: () => void
}

export function useScrollSync(
  enScroller: Ref<HTMLElement | null>,
  zhScroller: Ref<HTMLElement | null>,
): ScrollSync {
  let enOffsets: number[] = []
  let zhOffsets: number[] = []
  let enabled = false
  /** 程序滚动互斥锁（锁内触发的 scroll 事件忽略） */
  let lockEn = false
  let lockZh = false
  const lockTimers: number[] = []

  function measureOffsets(scroller: HTMLElement): number[] {
    const offsets: number[] = []
    scroller.querySelectorAll<HTMLElement>('[data-seq]').forEach((el) => {
      offsets[Number(el.dataset.seq)] = el.offsetTop
    })
    return offsets
  }

  /** 锚句：第一个 offsetTop >= scrollTop + 视口高*0.25 的索引（无则最后一句） */
  function anchorIndex(offsets: number[], scrollTop: number, viewportHeight: number): number {
    if (!offsets.length) return 0
    const threshold = scrollTop + viewportHeight * 0.25
    let lo = 0
    let hi = offsets.length
    while (lo < hi) {
      const mid = (lo + hi) >> 1
      if ((offsets[mid] ?? Infinity) < threshold) lo = mid + 1
      else hi = mid
    }
    return Math.min(lo, offsets.length - 1)
  }

  function scrollToIndex(scroller: HTMLElement, index: number, side: 'en' | 'zh') {
    const target = scroller.querySelector<HTMLElement>(`[data-seq="${index}"]`)
    if (!target) return
    if (side === 'en') lockEn = true
    else lockZh = true
    target.scrollIntoView({ block: 'start', behavior: 'auto' })
    // 锁窗口：scrollIntoView 触发的 scroll 事件在当帧内，120ms 足够
    const timer = window.setTimeout(() => {
      if (side === 'en') lockEn = false
      else lockZh = false
    }, 120)
    lockTimers.push(timer)
  }

  function onEnScroll() {
    if (!enabled || lockEn || !enScroller.value || !zhScroller.value) return
    const idx = anchorIndex(enOffsets, enScroller.value.scrollTop, enScroller.value.clientHeight)
    scrollToIndex(zhScroller.value, idx, 'zh')
  }

  function onZhScroll() {
    if (!enabled || lockZh || !zhScroller.value || !enScroller.value) return
    const idx = anchorIndex(zhOffsets, zhScroller.value.scrollTop, zhScroller.value.clientHeight)
    scrollToIndex(enScroller.value, idx, 'en')
  }

  function setEnabled(value: boolean) {
    if (value === enabled) return
    enabled = value
    const en = enScroller.value
    const zh = zhScroller.value
    if (enabled) {
      en?.addEventListener('scroll', onEnScroll, { passive: true })
      zh?.addEventListener('scroll', onZhScroll, { passive: true })
    } else {
      en?.removeEventListener('scroll', onEnScroll)
      zh?.removeEventListener('scroll', onZhScroll)
    }
  }

  async function measure() {
    await nextTick()
    if (enScroller.value) enOffsets = measureOffsets(enScroller.value)
    if (zhScroller.value) zhOffsets = measureOffsets(zhScroller.value)
  }

  function dispose() {
    setEnabled(false)
    lockTimers.forEach((t) => window.clearTimeout(t))
  }

  return { measure, setEnabled, dispose }
}
