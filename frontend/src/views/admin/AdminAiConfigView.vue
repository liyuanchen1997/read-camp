<template>
  <div class="ai-config">
    <h2 class="page-title">AI 模型配置</h2>
    <p class="desc">
      支持任意 OpenAI 兼容接口（DeepSeek、通义、本地 Ollama / LM Studio 等）。
      保存后立即生效，下次生成任务使用新配置。
    </p>

    <el-form :model="form" label-width="120px" style="max-width: 640px" v-loading="loading">
      <el-form-item label="接口地址" required>
        <el-input v-model.trim="form.baseUrl" placeholder="https://api.deepseek.com 或 http://localhost:11434/v1" />
      </el-form-item>
      <el-form-item label="API Key">
        <el-input
          v-model.trim="form.apiKey"
          type="password"
          show-password
          placeholder="本地模型可留空"
        />
      </el-form-item>
      <el-form-item label="模型名" required>
        <el-input v-model.trim="form.model" placeholder="deepseek-v4-flash / qwen-max / llama3 等" />
      </el-form-item>
      <el-form-item label="批量大小">
        <el-input-number v-model="form.batchSize" :min="1" :max="20" />
        <span class="hint">每批句子数（1-20），模型思考较长时建议调小</span>
      </el-form-item>
      <el-form-item label="温度">
        <el-slider v-model="form.temperature" :min="0" :max="1.5" :step="0.1" style="width: 240px" />
        <span class="hint">0.3 推荐；越高越有创造性，越低越稳定</span>
      </el-form-item>
      <el-form-item label="超时（秒）">
        <el-input-number v-model="form.timeoutSeconds" :min="10" :max="600" />
      </el-form-item>

      <el-form-item>
        <el-button type="primary" :loading="testing" @click="test">测试连接</el-button>
        <el-button type="primary" plain :loading="saving" @click="save">保存配置</el-button>
        <el-button @click="resetForm">重置</el-button>
      </el-form-item>

      <el-form-item v-if="testResult !== null">
        <el-alert
          :type="testResult.ok ? 'success' : 'error'"
          :title="testResult.ok ? '连接成功' : '连接失败'"
          :description="testResult.reply"
          :closable="false"
        />
      </el-form-item>
    </el-form>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { adminApi, type AiConfig } from '@/api/admin'

const form = reactive<AiConfig>({
  baseUrl: '',
  apiKey: '',
  model: '',
  batchSize: 3,
  temperature: 0.3,
  timeoutSeconds: 120,
})

const loading = ref(true)
const saving = ref(false)
const testing = ref(false)
const testResult = ref<{ ok: boolean; reply: string } | null>(null)

async function load() {
  try {
    const config = await adminApi.getAiConfig()
    Object.assign(form, config)
  } catch {
    ElMessage.error('加载配置失败')
  } finally {
    loading.value = false
  }
}

async function test() {
  testing.value = true
  testResult.value = null
  try {
    const result = await adminApi.testAiConfig({ ...form })
    testResult.value = result
    if (result.ok) ElMessage.success('连接成功')
    else ElMessage.error('连接失败')
  } catch (e) {
    testResult.value = { ok: false, reply: (e as Error).message || '连接失败' }
    ElMessage.error('连接失败')
  } finally {
    testing.value = false
  }
}

async function save() {
  if (!form.baseUrl.trim() || !form.model.trim()) {
    ElMessage.warning('接口地址与模型名不能为空')
    return
  }
  saving.value = true
  try {
    await adminApi.updateAiConfig({ ...form })
    ElMessage.success('配置已保存，下次生成生效')
  } catch (e) {
    ElMessage.error((e as Error).message || '保存失败')
  } finally {
    saving.value = false
  }
}

function resetForm() {
  load()
}

onMounted(load)
</script>

<style scoped>
.page-title {
  font-family: var(--font-serif-zh);
  font-size: 1.3rem;
  margin-bottom: var(--space-2);
}

.desc {
  color: var(--ink-2);
  font-size: 0.85rem;
  margin-bottom: var(--space-4);
}

.hint {
  margin-left: var(--space-2);
  color: var(--ink-3);
  font-size: 0.75rem;
}
</style>
