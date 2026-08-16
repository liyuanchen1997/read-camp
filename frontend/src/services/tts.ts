/**
 * TTS 控制器（浏览器原生 speechSynthesis，doc/00-design.md §4）
 * - 整篇顺序朗读：currentIndex 推进 + 每句 onend 回调（调用方负责高亮/滚动跟随）
 * - 语音选择：en-US → en-* → 默认；无英文音色提示一次；音色/语速 localStorage 持久化
 * - pause 在 Chrome 不可靠：resume 失败时降级为 cancel + 当前句重播
 */

export type TtsState = 'idle' | 'playing' | 'paused'

const VOICE_KEY = 'readcamp-voice'
const RATE_KEY = 'readcamp-rate'

class TtsController {
  private state: TtsState = 'idle'
  private currentIndex = -1
  private utterance: SpeechSynthesisUtterance | null = null
  private rate = 1
  private voices: SpeechSynthesisVoice[] = []
  private voiceWarned = false

  /** 整篇朗读：当前句推进回调（index: -1 表示结束） */
  onProgress: ((index: number) => void) | null = null

  constructor() {
    const savedRate = Number(localStorage.getItem(RATE_KEY))
    if (savedRate >= 0.5 && savedRate <= 1.5) this.rate = savedRate

    // getVoices 异步加载
    this.loadVoices()
    if ('onvoiceschanged' in speechSynthesis) {
      speechSynthesis.onvoiceschanged = () => this.loadVoices()
    }
  }

  private loadVoices() {
    this.voices = speechSynthesis.getVoices()
    if (this.voices.length && !this.pickVoice()) {
      // 已加载且无英文音色 → 提示一次（中文系统兜底）
      if (!this.voiceWarned) {
        this.voiceWarned = true
        console.warn('[tts] 未找到英文语音，将使用系统默认语音')
      }
    }
  }

  /** 常见女性英文语音名（macOS/Chrome 系统语音） */
  private static readonly FEMALE_VOICES = [
    'samantha', 'karen', 'moira', 'victoria', 'allison', 'ava', 'joanna',
    'salli', 'kimberly', 'kendra', 'zira', 'hazel', 'susan', 'catherine',
    'fiona', 'serena', 'melina', 'tessa', 'veena', 'emma', 'lisa', 'ivy',
  ]

  /** 选择语音：持久化选择 → en-US 女声 → en-* 女声 → en-US → en-* → null（默认） */
  private pickVoice(): SpeechSynthesisVoice | null {
    const saved = localStorage.getItem(VOICE_KEY)
    if (saved) {
      const v = this.voices.find((item) => item.voiceURI === saved)
      if (v) return v
    }
    const female = this.voices.filter((v) =>
      TtsController.FEMALE_VOICES.some((name) => v.name.toLowerCase().includes(name)))
    const enUSFemale = female.find((v) => v.lang === 'en-US')
    if (enUSFemale) return enUSFemale
    const enFemale = female.find((v) => v.lang.startsWith('en'))
    if (enFemale) return enFemale
    const enUS = this.voices.find((v) => v.lang === 'en-US')
    if (enUS) return enUS
    return this.voices.find((v) => v.lang.startsWith('en')) ?? null
  }

  private makeUtterance(text: string) {
    const u = new SpeechSynthesisUtterance(text)
    const voice = this.pickVoice()
    if (voice) {
      u.voice = voice
      u.lang = voice.lang
    } else {
      u.lang = 'en-US'
    }
    u.rate = this.rate
    return u
  }

  /** 独立朗读（单句/单词）：先打断当前播放 */
  speak(text: string) {
    speechSynthesis.cancel()
    this.stopSequence()
    const u = this.makeUtterance(text)
    speechSynthesis.speak(u)
  }

  /** 整篇朗读：从 index 开始顺序播放 */
  playSequence(index: number, texts: string[]) {
    this.cancelAll()
    this.currentIndex = index
    this.state = 'playing'
    this.playOne(texts)
  }

  private playOne(texts: string[]) {
    if (this.state !== 'playing') return
    if (this.currentIndex >= texts.length) {
      this.finishSequence()
      return
    }
    const u = this.makeUtterance(texts[this.currentIndex])
    this.utterance = u
    this.onProgress?.(this.currentIndex)
    u.onend = () => {
      this.currentIndex++
      this.playOne(texts)
    }
    u.onerror = () => {
      // 单句出错跳过，继续下一句
      this.currentIndex++
      this.playOne(texts)
    }
    speechSynthesis.speak(u)
  }

  private finishSequence() {
    this.state = 'idle'
    this.currentIndex = -1
    this.utterance = null
    this.onProgress?.(-1)
  }

  private stopSequence() {
    this.state = 'idle'
    this.currentIndex = -1
    this.utterance = null
    this.onProgress?.(-1)
  }

  pause() {
    if (this.state !== 'playing') return
    speechSynthesis.pause()
    this.state = 'paused'
  }

  /** 继续：resume 不可靠（Chrome）→ 降级 cancel + 当前句重播 */
  resume(texts: string[]) {
    if (this.state !== 'paused') return
    speechSynthesis.resume()
    // Chrome pause/resume 不稳定：短暂后确认仍在说话，否则重播
    window.setTimeout(() => {
      if (this.state === 'paused' && !speechSynthesis.speaking) {
        speechSynthesis.cancel()
        this.state = 'playing'
        this.playOne(texts)
      }
    }, 250)
  }

  /** 停止并复位 */
  stop() {
    this.cancelAll()
    this.stopSequence()
  }

  private cancelAll() {
    speechSynthesis.cancel()
    this.utterance = null
  }

  getRate() {
    return this.rate
  }

  setRate(value: number) {
    this.rate = value
    localStorage.setItem(RATE_KEY, String(value))
    // 播放中更新语速：重播当前句
    if (this.utterance) {
      const text = this.utterance.text
      speechSynthesis.cancel()
      this.utterance = this.makeUtterance(text)
      speechSynthesis.speak(this.utterance)
    }
  }

  getState() {
    return this.state
  }

  /** 备选英文语音列表（设置语音用，步骤 12 可选） */
  getEnglishVoices() {
    return this.voices.filter((v) => v.lang.startsWith('en'))
  }
}

export const tts = new TtsController()
