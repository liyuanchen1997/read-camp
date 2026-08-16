<template>
  <div class="editor">
    <h2 class="page-title">{{ isEdit ? '编辑文章' : '新建文章' }}</h2>

    <el-form :model="form" label-width="80px" style="max-width: 860px">
      <el-form-item label="标题" required>
        <el-input v-model="form.title" maxlength="200" placeholder="文章标题" />
      </el-form-item>
      <el-form-item label="简介">
        <el-input v-model="form.summary" maxlength="500" type="textarea" :rows="2" placeholder="一句话简介" />
      </el-form-item>
      <el-form-item label="难度" required>
        <el-radio-group v-model="form.difficulty">
          <el-radio :value="1">入门</el-radio>
          <el-radio :value="2">进阶</el-radio>
          <el-radio :value="3">挑战</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="标签">
        <el-select
          v-model="form.tags"
          multiple
          filterable
          allow-create
          default-first-option
          placeholder="回车添加标签（最多 5 个）"
          style="width: 100%"
        >
          <el-option v-for="t in form.tags ?? []" :key="t" :label="t" :value="t" />
        </el-select>
      </el-form-item>
      <!-- 章节编辑：每章标题 + 正文，可增删/排序（恒 ≥1 章） -->
      <el-form-item label="章节" required>
        <div class="chapter-list">
          <div v-for="(ch, i) in form.chapters" :key="i" class="chapter-card">
            <div class="chapter-head">
              <span class="chapter-badge">第 {{ i + 1 }} 章</span>
              <el-input
                v-model="ch.title"
                maxlength="200"
                placeholder="章节标题（留空保存时自动补第 N 章）"
                style="flex: 1"
              />
              <el-button size="small" text :disabled="i === 0" title="上移" @click="moveChapter(i, -1)">↑</el-button>
              <el-button size="small" text :disabled="i === form.chapters.length - 1" title="下移" @click="moveChapter(i, 1)">↓</el-button>
              <el-button size="small" text type="danger" :disabled="form.chapters.length <= 1" title="删除章节" @click="removeChapter(i)">✕</el-button>
            </div>
            <el-input
              v-model="ch.content"
              type="textarea"
              :rows="10"
              placeholder="英文原文（段落之间用空行分隔，保存时自动切分句子）"
            />
          </div>
          <el-button size="small" @click="addChapter">＋ 添加章节</el-button>
        </div>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
        <el-button @click="router.back()">取消</el-button>
        <span v-if="lastSplit" class="split-info">
          上次保存切分：{{ lastSplit }} 句 / {{ lastSplitWords }} 词
        </span>
      </el-form-item>
    </el-form>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminApi, type ChapterForm } from '@/api/admin'

const route = useRoute()
const router = useRouter()

const id = computed(() => Number(route.params.id) || 0)
const isEdit = computed(() => id.value > 0)

const form = reactive({
  title: '',
  summary: '',
  chapters: [{ title: '', content: '' }] as ChapterForm[],
  tags: [] as string[],
  difficulty: 1 as 1 | 2 | 3,
})

const saving = ref(false)
const lastSplit = ref<number | null>(null)
const lastSplitWords = ref<number | null>(null)

/** 各章正文 trim 后空行拼接（与后端 joinChapters 逐字节一致） */
const joinedContent = computed(() =>
  form.chapters
    .map((c) => c.content.trim())
    .join('\n\n'),
)

async function load() {
  if (!isEdit.value) return
  try {
    const d = await adminApi.detail(id.value)
    form.title = d.title
    form.summary = d.summary
    form.chapters = d.chapters?.length
      ? d.chapters.map((c) => ({ title: c.title, content: c.content ?? '' }))
      : [{ title: d.title, content: d.contentEn }] // 旧文章回退单章
    form.tags = d.tags ?? []
    form.difficulty = d.difficulty as 1 | 2 | 3
  } catch {
    ElMessage.error('加载文章失败')
    router.push('/admin/articles')
  }
}

function addChapter() {
  form.chapters.push({ title: '', content: '' })
}

function removeChapter(i: number) {
  if (form.chapters.length <= 1) return
  form.chapters.splice(i, 1)
}

function moveChapter(i: number, dir: -1 | 1) {
  const j = i + dir
  if (j < 0 || j >= form.chapters.length) return
  const tmp = form.chapters[i]
  form.chapters[i] = form.chapters[j]
  form.chapters[j] = tmp
}

async function save() {
  if (!form.title.trim()) {
    ElMessage.warning('标题不能为空')
    return
  }
  if (!joinedContent.value.trim()) {
    ElMessage.warning('正文不能为空')
    return
  }
  // 章标题留空自动补"第 N 章"
  form.chapters.forEach((c, i) => {
    if (!c.title.trim()) c.title = `第 ${i + 1} 章`
  })
  // 编辑且正文/章节结构变更：重切分会清空标注与用户进度，需确认（仅章标题变化不弹）
  if (isEdit.value) {
    const original = await adminApi.detail(id.value)
    if (original.contentEn.trim() !== joinedContent.value.trim()) {
      try {
        await ElMessageBox.confirm(
          '正文已修改（含章节增删/排序）：保存后将重新切分句子，清空已有 AI 标注与全部用户的阅读进度。确定继续？',
          '重切分确认',
          { type: 'warning', confirmButtonText: '确定重切分', cancelButtonText: '取消' },
        )
      } catch {
        return // 用户取消
      }
    }
  }

  saving.value = true
  try {
    const payload = {
      title: form.title.trim(),
      summary: form.summary.trim(),
      content: joinedContent.value,
      chapters: form.chapters.map((c) => ({ title: c.title.trim(), content: c.content.trim() })),
      tags: form.tags.length ? form.tags : undefined,
      difficulty: form.difficulty,
    }
    const saved = isEdit.value ? await adminApi.update(id.value, payload) : await adminApi.create(payload)
    lastSplit.value = saved.sentenceCount
    lastSplitWords.value = saved.wordCount
    ElMessage.success(`保存成功：切分 ${saved.sentenceCount} 句 / ${saved.wordCount} 词`)
    if (!isEdit.value) {
      router.replace(`/admin/articles/${saved.id}/edit`)
    }
  } catch (e) {
    ElMessage.error((e as Error).message || '保存失败')
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.page-title {
  font-family: var(--font-serif-zh);
  font-size: 1.3rem;
  margin-bottom: var(--space-4);
}

.chapter-list {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}

.chapter-card {
  border: 1px solid var(--line);
  border-radius: var(--radius-sm);
  padding: var(--space-3);
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
  background: var(--bg-card);
}

.chapter-head {
  display: flex;
  align-items: center;
  gap: var(--space-2);
}

.chapter-badge {
  font-family: var(--font-serif-zh);
  font-size: 0.85rem;
  font-weight: 600;
  color: var(--accent);
  flex-shrink: 0;
}

.split-info {
  margin-left: var(--space-3);
  color: var(--ink-3);
  font-size: 0.82rem;
}
</style>
