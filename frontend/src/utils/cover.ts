/** 无封面时按标题 hash 生成暖色渐变（doc/01-ui-design.md §3.3） */
const PALETTES = [
  ['#B8860B', '#D4A017'],
  ['#3F5A45', '#6B8F71'],
  ['#8B5E3C', '#C49A6C'],
  ['#5B4A6B', '#8E7BA3'],
  ['#9C6B4E', '#D8B08C'],
  ['#4A6B8A', '#7BA3C4'],
]

export function coverGradient(title: string): string {
  let hash = 0
  for (let i = 0; i < title.length; i++) {
    hash = (hash * 31 + title.charCodeAt(i)) >>> 0
  }
  const [from, to] = PALETTES[hash % PALETTES.length]
  return `linear-gradient(135deg, ${from} 0%, ${to} 100%)`
}

/** 封面水印字母：标题首字母（或首个单词首字母）大写 */
export function coverInitial(title: string): string {
  const first = title.trim().charAt(0)
  return first ? first.toUpperCase() : 'R'
}
