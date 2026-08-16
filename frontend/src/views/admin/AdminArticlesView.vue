<template>
  <div class="admin-articles">
    <div class="toolbar">
      <h2 class="page-title">文章管理</h2>
      <div class="toolbar-actions">
        <el-radio-group v-model="statusFilter" size="small" @change="reload">
          <el-radio-button :value="0">全部</el-radio-button>
          <el-radio-button :value="1">已上架</el-radio-button>
          <el-radio-button :value="2">已下架</el-radio-button>
        </el-radio-group>
        <el-input
          v-model="keyword"
          placeholder="搜索标题…"
          clearable
          style="width: 200px"
          @keyup.enter="reload"
          @clear="reload"
        />
        <el-button type="primary" size="small" @click="reload">搜索</el-button>
        <el-button type="primary" @click="router.push('/admin/articles/new')">＋ 新建文章</el-button>
      </div>
    </div>

    <el-table :data="articles" v-loading="loading" stripe>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="title" label="标题" min-width="200" show-overflow-tooltip />
      <el-table-column label="难度" width="80">
        <template #default="{ row }">
          <el-tag :type="difficultyType(row.difficulty)" size="small">
            {{ difficultyLabel(row.difficulty) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="句子" width="80">
        <template #default="{ row }">{{ row.sentenceCount }} 句</template>
      </el-table-column>
      <el-table-column label="AI 标注" width="170">
        <template #default="{ row }">
          <span v-if="genMap[row.id]" class="gen-cell">
            <el-tag v-if="genMap[row.id].running" type="warning" size="small" effect="dark">
              ⏳ 生成中
            </el-tag>
            <span>{{ genMap[row.id].done }}/{{ genMap[row.id].total }}</span>
            <el-tag v-if="genMap[row.id].failed > 0" type="danger" size="small">
              {{ genMap[row.id].failed }} 失败
            </el-tag>
          </span>
          <span v-else class="muted">—</span>
        </template>
      </el-table-column>
      <el-table-column label="上架" width="80">
        <template #default="{ row }">
          <el-switch
            :model-value="row.status === 1"
            @change="toggleStatus(row, $event)"
          />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="230" fixed="right">
        <template #default="{ row }">
          <el-button size="small" type="primary" plain @click="openGen(row)">生成标注</el-button>
          <el-button size="small" @click="router.push(`/admin/articles/${row.id}/edit`)">编辑</el-button>
          <el-popconfirm title="删除文章将级联删除句子/标注/进度/收藏，确认？" @confirm="remove(row)">
            <template #reference>
              <el-button size="small" type="danger" plain>删除</el-button>
            </template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>

    <div class="pager">
      <el-pagination
        v-model:current-page="page"
        :page-size="size"
        :total="total"
        layout="prev, pager, next"
        @current-change="fetchPage"
      />
    </div>

    <!-- 生成对话框 -->
    <GenProgressDialog
      v-model="genDialogOpen"
      :article="genArticle"
      @refresh-list="loadGenSummaries"
    />
  </div>
</template>

<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { adminApi, type GenStatus } from '@/api/admin'
import type { ArticleDto } from '@/api/article'
import GenProgressDialog from '@/components/admin/GenProgressDialog.vue'

const router = useRouter()

const articles = ref<ArticleDto[]>([])
const statusFilter = ref(0) // 0 全部 / 1 已上架 / 2 已下架
const keyword = ref('')
const page = ref(1)
const size = 10
const total = ref(0)
const loading = ref(false)

const genMap = ref<Record<number, GenStatus>>({})

const genDialogOpen = ref(false)
const genArticle = ref<ArticleDto | null>(null)

function difficultyLabel(d: number) {
  return { 1: '入门', 2: '进阶', 3: '挑战' }[d] ?? '—'
}

function difficultyType(d: number) {
  return ({ 1: 'success', 2: 'warning', 3: 'danger' } as const)[d] ?? 'info'
}

async function fetchPage() {
  loading.value = true
  try {
    const data = await adminApi.list({
      status: statusFilter.value === 0 ? undefined : statusFilter.value === 1 ? 1 : 0,
      keyword: keyword.value || undefined,
      page: page.value,
      size,
    })
    articles.value = data.records
    total.value = data.total
    await loadGenSummaries()
  } catch {
    ElMessage.error('加载文章列表失败')
  } finally {
    loading.value = false
  }
}

/** 批量拉取生成进度汇总（文章数量少，逐篇查询）；任一任务运行中则 2s 轮询跟随 */
async function loadGenSummaries() {
  const entries = await Promise.all(
    articles.value.map(async (a) => {
      try {
        return [a.id, await adminApi.genStatus(a.id)] as const
      } catch {
        return [a.id, null] as const
      }
    }),
  )
  genMap.value = Object.fromEntries(entries.filter((e) => e[1] !== null))
  if (Object.values(genMap.value).some((s) => s.running)) schedulePoll()
}

let pollTimer: number | null = null

function schedulePoll() {
  if (pollTimer !== null) return
  pollTimer = window.setTimeout(async () => {
    pollTimer = null
    await loadGenSummaries()
  }, 2000)
}

onBeforeUnmount(() => {
  if (pollTimer !== null) window.clearTimeout(pollTimer)
})

function reload() {
  page.value = 1
  fetchPage()
}

async function toggleStatus(row: ArticleDto, val: unknown) {
  const on = val === true
  try {
    await adminApi.changeStatus(row.id, on ? 1 : 0)
    row.status = on ? 1 : 0
    ElMessage.success(on ? '已上架' : '已下架')
  } catch {
    ElMessage.error('操作失败')
  }
}

async function remove(row: ArticleDto) {
  try {
    await adminApi.remove(row.id)
    ElMessage.success('已删除')
    fetchPage()
  } catch {
    ElMessage.error('删除失败')
  }
}

function openGen(row: ArticleDto) {
  genArticle.value = row
  genDialogOpen.value = true
}

onMounted(fetchPage)
</script>

<style scoped>
.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--space-4);
  flex-wrap: wrap;
  gap: var(--space-3);
}

.toolbar-actions {
  display: flex;
  align-items: center;
  gap: var(--space-2);
}

.page-title {
  font-family: var(--font-serif-zh);
  font-size: 1.3rem;
}

.muted {
  color: var(--ink-3);
}

.gen-cell {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: var(--space-4);
}
</style>
