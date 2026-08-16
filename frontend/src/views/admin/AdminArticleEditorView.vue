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
      <el-form-item label="正文" required>
        <el-input
          v-model="form.content"
          type="textarea"
          :rows="16"
          placeholder="英文原文（段落之间用空行分隔，保存时自动切分句子）"
        />
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
import { adminApi } from '@/api/admin'

const route = useRoute()
const router = useRouter()

const id = computed(() => Number(route.params.id) || 0)
const isEdit = computed(() => id.value > 0)

const form = reactive({
  title: '',
  summary: '',
  content: '',
  tags: [] as string[],
  difficulty: 1 as 1 | 2 | 3,
})

const saving = ref(false)
const lastSplit = ref<number | null>(null)
const lastSplitWords = ref<number | null>(null)

async function load() {
  if (!isEdit.value) return
  try {
    const d = await adminApi.detail(id.value)
    form.title = d.title
    form.summary = d.summary
    form.content = d.contentEn
    form.tags = d.tags ?? []
    form.difficulty = d.difficulty as 1 | 2 | 3
  } catch {
    ElMessage.error('加载文章失败')
    router.push('/admin/articles')
  }
}

async function save() {
  if (!form.title.trim() || !form.content.trim()) {
    ElMessage.warning('标题与正文不能为空')
    return
  }
  // 编辑且正文变更：重切分会清空标注与用户进度，需确认
  if (isEdit.value) {
    const original = await adminApi.detail(id.value)
    if (original.contentEn !== form.content) {
      try {
        await ElMessageBox.confirm(
          '正文已修改：保存后将重新切分句子，清空已有 AI 标注与全部用户的阅读进度。确定继续？',
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
      content: form.content,
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

.split-info {
  margin-left: var(--space-3);
  color: var(--ink-3);
  font-size: 0.82rem;
}
</style>
