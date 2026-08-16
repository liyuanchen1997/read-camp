<template>
  <el-dialog
    :model-value="modelValue"
    :title="`生成标注 · ${article?.title ?? ''}`"
    width="640px"
    :close-on-click-modal="false"
    @update:model-value="emit('update:modelValue', $event)"
    @closed="onClosed"
  >
    <!-- 未启动：选择模式 -->
    <div v-if="!started">
      <el-radio-group v-model="target">
        <el-radio value="missing">仅生成未生成的句子（增量，推荐）</el-radio>
        <el-radio value="all">全部重新生成（清空已有标注）</el-radio>
      </el-radio-group>
      <p class="tip">
        {{ modelName }}按需生成，预计每句约 2000+ tokens；本文共
        <b>{{ article?.sentenceCount ?? 0 }}</b> 句，建议先生成少量验证效果。
      </p>
    </div>

    <!-- 进行中/已完成 -->
    <div v-else>
      <!-- 状态计数 -->
      <div class="counts">
        <div class="count">
          <span class="num pending">{{ status?.pending ?? 0 }}</span>
          <span class="label">未生成</span>
        </div>
        <div class="count">
          <span class="num generating">{{ status?.generating ?? 0 }}</span>
          <span class="label">生成中</span>
        </div>
        <div class="count">
          <span class="num done">{{ status?.done ?? 0 }}</span>
          <span class="label">已生成</span>
        </div>
        <div class="count">
          <span class="num failed">{{ status?.failed ?? 0 }}</span>
          <span class="label">失败</span>
        </div>
      </div>

      <!-- 进度条 -->
      <el-progress
        :percentage="percent"
        :status="status?.running ? undefined : status?.failed ? 'exception' : 'success'"
      />

      <p v-if="status?.running" class="tip running-tip">任务进行中，关闭窗口不会中断，稍后回来查看即可…</p>

      <!-- 失败句列表 -->
      <div v-if="failedSentences.length" class="failed-list">
        <h4 class="failed-title">失败句子（点击重试）</h4>
        <div v-for="f in failedSentences" :key="f.sentenceId" class="failed-item">
          <span class="failed-seq">#{{ f.seq + 1 }}</span>
          <span class="failed-err" :title="f.genError ?? ''">{{ f.genError || '生成失败' }}</span>
          <el-button size="small" type="danger" plain :loading="retryingId === f.sentenceId" @click="retry(f)">
            重试
          </el-button>
        </div>
      </div>

    </div>

    <!-- footer（必须作为 el-dialog 直接子级） -->
    <template #footer>
      <template v-if="!started">
        <el-button @click="emit('update:modelValue', false)">关闭</el-button>
        <el-button type="primary" :loading="starting" @click="start">
          {{ status?.failed ? '重新生成失败句' : '开始生成' }}
        </el-button>
      </template>
      <template v-else>
        <el-button v-if="status?.running" @click="cancel">取消任务</el-button>
        <el-button v-else type="primary" @click="emit('update:modelValue', false)">完成</el-button>
      </template>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { adminApi, type AiConfig, type GenStatus } from '@/api/admin'
import type { ArticleDto } from '@/api/article'

const props = defineProps<{
  modelValue: boolean
  article: ArticleDto | null
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  'refresh-list': []
}>()

const target = ref<'missing' | 'all'>('missing')
const started = ref(false)
const starting = ref(false)
const status = ref<GenStatus | null>(null)
const retryingId = ref<number | null>(null)

/** 当前配置的 AI 模型名（提示语跟随配置，不写死厂商） */
const modelName = ref('AI 模型')

let pollTimer: number | null = null

const percent = computed(() => {
  if (!status.value || status.value.total === 0) return 0
  return Math.round(((status.value.done + status.value.failed) / status.value.total) * 100)
})

const failedSentences = computed(
  () => status.value?.perSentence.filter((s) => s.genStatus === 3) ?? [],
)

async function start() {
  if (!props.article) return
  starting.value = true
  try {
    const { total } = await adminApi.generate(props.article.id, target.value)
    if (total === 0) {
      ElMessage.info('无需生成：所有句子均已生成')
      started.value = false
      return
    }
    started.value = true
    poll()
    ElMessage.success(`已启动生成，共 ${total} 句（页面关闭不中断）`)
  } catch (e) {
    ElMessage.error((e as Error).message || '启动失败')
  } finally {
    starting.value = false
  }
}

/** 2s 轮询进度 */
async function poll() {
  if (!props.article) return
  try {
    status.value = await adminApi.genStatus(props.article.id)
    if (status.value.running) {
      pollTimer = window.setTimeout(poll, 2000)
    } else {
      emit('refresh-list')
    }
  } catch {
    pollTimer = window.setTimeout(poll, 3000)
  }
}

async function cancel() {
  if (!props.article) return
  try {
    await adminApi.cancel(props.article.id)
    ElMessage.info('已请求取消（当前批次完成后停止）')
  } catch {
    ElMessage.error('取消失败')
  }
}

async function retry(f: { sentenceId: number }) {
  if (!props.article) return
  retryingId.value = f.sentenceId
  try {
    await adminApi.generateOne(props.article.id, f.sentenceId)
    ElMessage.success('已重新生成该句')
    poll()
  } catch (e) {
    ElMessage.error((e as Error).message || '重试失败')
  } finally {
    retryingId.value = null
  }
}

function stopPoll() {
  if (pollTimer !== null) {
    window.clearTimeout(pollTimer)
    pollTimer = null
  }
}

function onClosed() {
  stopPoll()
  started.value = false
  status.value = null
}

// 打开对话框时同步一次进度（已生成过的文章），并读取当前 AI 模型名
watch(
  () => props.modelValue,
  (open) => {
    if (open && props.article) {
      adminApi.genStatus(props.article.id).then((s) => {
        status.value = s
        started.value = s.running
      })
      adminApi.getAiConfig().then((c: AiConfig) => {
        if (c.model) modelName.value = c.model
      }).catch(() => {})
    }
  },
)

onBeforeUnmount(stopPoll)
</script>

<style scoped>
.tip {
  color: var(--ink-3);
  font-size: 0.82rem;
  margin-top: var(--space-3);
  line-height: 1.7;
}

.counts {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: var(--space-3);
  margin-bottom: var(--space-4);
}

.count {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
  padding: var(--space-3);
  border: 1px solid var(--line);
  border-radius: var(--radius-sm);
}

.count .num {
  font-size: 1.4rem;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
}

.count .num.pending {
  color: var(--ink-3);
}

.count .num.generating {
  color: var(--accent);
}

.count .num.done {
  color: var(--green);
}

.count .num.failed {
  color: var(--danger);
}

.count .label {
  font-size: 0.72rem;
  color: var(--ink-2);
}

.running-tip {
  margin-top: var(--space-3);
  color: var(--accent);
}

.failed-list {
  margin-top: var(--space-4);
  max-height: 220px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}

.failed-title {
  font-size: 0.9rem;
  color: var(--danger);
}

.failed-item {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  padding: 6px 10px;
  border: 1px solid rgba(179, 64, 46, 0.25);
  border-radius: var(--radius-sm);
  background: rgba(179, 64, 46, 0.04);
}

.failed-seq {
  font-weight: 600;
  color: var(--danger);
  flex-shrink: 0;
}

.failed-err {
  flex: 1;
  font-size: 0.8rem;
  color: var(--ink-2);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
