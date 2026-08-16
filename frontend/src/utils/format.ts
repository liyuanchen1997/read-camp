/** 相对时间：刚刚 / N 分钟前 / N 小时前 / N 天前 / 日期 */
export function formatRelativeTime(iso: string): string {
  const date = new Date(iso)
  const diffMs = Date.now() - date.getTime()
  const minute = 60 * 1000
  const hour = 60 * minute
  const day = 24 * hour

  if (diffMs < minute) return '刚刚'
  if (diffMs < hour) return `${Math.floor(diffMs / minute)} 分钟前`
  if (diffMs < day) return `${Math.floor(diffMs / hour)} 小时前`
  if (diffMs < 30 * day) return `${Math.floor(diffMs / day)} 天前`

  const y = date.getFullYear()
  const m = String(date.getMonth() + 1).padStart(2, '0')
  const d = String(date.getDate()).padStart(2, '0')
  return `${y}-${m}-${d}`
}

/** 难度徽章文案与颜色类名 */
export const DIFFICULTY = {
  1: { label: '入门', className: 'diff-1' },
  2: { label: '进阶', className: 'diff-2' },
  3: { label: '挑战', className: 'diff-3' },
} as const
