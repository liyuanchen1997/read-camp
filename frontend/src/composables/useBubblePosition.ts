import { nextTick, onBeforeUnmount, onMounted, ref, watch, type Ref } from 'vue'
import type { CSSProperties } from 'vue'
import { bubbleStyle } from '@/utils/bubble'

/**
 * 气泡定位 composable（fixed 锚定 + 翻转 + 视口内完整可见）
 * 关键：测量时序——Teleport 内容在挂载前 bubbleEl 不可用，单次测量会拿到
 * 默认高度导致翻转不触发；因此挂载后重测 + ResizeObserver 监听尺寸变化持续校正。
 */
export function useBubblePosition(
  anchor: Ref<DOMRect>,
  bubbleEl: Ref<HTMLElement | null>,
  width = 460,
) {
  const style = ref<CSSProperties>({ position: 'fixed', left: '-9999px' })
  let ro: ResizeObserver | null = null

  function update() {
    if (!bubbleEl.value) return
    style.value = bubbleStyle(anchor.value, bubbleEl.value, width)
  }

  watch(
    anchor,
    async () => {
      await nextTick()
      update()
    },
    { immediate: true },
  )

  onMounted(() => {
    // 挂载后 bubbleEl 必定可用：重测修正初始定位（可能在 -9999px 处测量）
    update()
    // 内容/样式变化（如 maxHeight 生效）后持续校正翻转与贴底
    if (typeof ResizeObserver !== 'undefined') {
      ro = new ResizeObserver(() => update())
      if (bubbleEl.value) ro.observe(bubbleEl.value)
    }
  })

  onBeforeUnmount(() => ro?.disconnect())

  return { style }
}
