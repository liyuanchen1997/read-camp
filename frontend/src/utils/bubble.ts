import type { CSSProperties } from 'vue'

/**
 * 气泡定位（doc/00-design.md §4）：fixed 锚定触发元素，
 * 底部空间不足自动上翻，左右越界回夹。
 */
export function bubbleStyle(
  anchor: DOMRect,
  bubbleEl: HTMLElement | null,
): CSSProperties {
  const vw = window.innerWidth
  const vh = window.innerHeight
  const gap = 8
  const width = Math.min(460, vw - 24)
  const height = bubbleEl?.offsetHeight ?? 200

  let top = anchor.bottom + gap
  if (top + height > vh - 12 && anchor.top - height - gap > 12) {
    top = anchor.top - height - gap
  }
  const left = Math.max(12, Math.min(anchor.left, vw - width - 12))

  return {
    position: 'fixed',
    top: `${top}px`,
    left: `${left}px`,
    width: `${width}px`,
    zIndex: 300,
  }
}
