import type { CSSProperties } from 'vue'

/**
 * 气泡定位（doc/00-design.md §4）：fixed 锚定触发元素，
 * 底部空间不足自动上翻，左右越界回夹。
 */
export function bubbleStyle(
  anchor: DOMRect,
  bubbleEl: HTMLElement | null,
  width = 460,
): CSSProperties {
  const vw = window.innerWidth
  // visualViewport：移动端地址栏/键盘收展时更精确的可视区（与 100dvh 口径一致）
  const vh = window.visualViewport?.height ?? window.innerHeight
  const gap = 8
  const finalWidth = Math.min(width, vw - 24)
  // 未测量到时用保守估计（触发翻转/贴底而非默认小值导致溢出）
  const height = bubbleEl?.offsetHeight ?? Math.min(400, vh * 0.5)

  let top = anchor.bottom + gap
  if (top + height > vh - 12) {
    if (anchor.top - height - gap > 12) {
      // 下方放不下且上方空间足够：上翻
      top = anchor.top - height - gap
    } else {
      // 上下都放不下（气泡接近满屏）：贴底部对齐，保证完整可见
      top = Math.max(12, vh - height - 12)
    }
  }
  const left = Math.max(12, Math.min(anchor.left, vw - finalWidth - 12))

  return {
    position: 'fixed',
    top: `${top}px`,
    left: `${left}px`,
    width: `${finalWidth}px`,
    // 内联限制最大高度（inline style 优先，与定位计算同一视口口径）：
    // 超长时气泡内部滚动，保证完整可见
    maxHeight: `${vh - 24}px`,
    overflowY: 'auto',
    zIndex: 300,
  }
}
