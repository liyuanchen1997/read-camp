<template>
  <div class="auth-page">
    <div class="auth-card">
      <h1 class="brand">英语精读训练营</h1>
      <p class="slogan">注册新读者账号</p>

      <form class="auth-form" @submit.prevent="onSubmit">
        <div class="field">
          <label for="username">用户名</label>
          <input
            id="username"
            v-model.trim="form.username"
            type="text"
            autocomplete="username"
            placeholder="3-20 个字符"
            required
          />
        </div>
        <div class="field">
          <label for="password">密码</label>
          <input
            id="password"
            v-model="form.password"
            type="password"
            autocomplete="new-password"
            placeholder="6-32 个字符"
            required
          />
        </div>
        <div class="field">
          <label for="nickname">昵称（可选）</label>
          <input
            id="nickname"
            v-model.trim="form.nickname"
            type="text"
            autocomplete="nickname"
            placeholder="默认使用用户名"
          />
        </div>

        <p v-if="errorMsg" class="error-msg" role="alert">{{ errorMsg }}</p>

        <button class="submit-btn" type="submit" :disabled="loading">
          {{ loading ? '注册中…' : '注 册' }}
        </button>
      </form>

      <p class="switch-tip">
        已有账号？
        <router-link :to="{ path: '/login', query: redirectQuery }">去登录</router-link>
      </p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const form = reactive({ username: '', password: '', nickname: '' })
const loading = ref(false)
const errorMsg = ref('')

const redirectQuery = computed(() =>
  typeof route.query.redirect === 'string' ? { redirect: route.query.redirect } : {},
)

async function onSubmit() {
  if (loading.value) return
  loading.value = true
  errorMsg.value = ''
  try {
    await userStore.register({
      username: form.username,
      password: form.password,
      nickname: form.nickname || undefined,
    })
    // 注册成功 → 自动跳登录（携带 redirect 回跳）
    router.push({ path: '/login', query: redirectQuery.value })
  } catch (e) {
    errorMsg.value = (e as Error).message || '注册失败，请重试'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.auth-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: var(--space-5);
  background:
    radial-gradient(circle at 20% 20%, rgba(184, 134, 11, 0.05), transparent 40%),
    var(--bg);
}

.auth-card {
  width: 100%;
  max-width: 420px;
  background: var(--bg-card);
  border: 1px solid var(--line);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow);
  padding: var(--space-7) var(--space-6) var(--space-5);
}

.brand {
  font-family: var(--font-serif-zh);
  font-size: 1.7rem;
  color: var(--accent);
  text-align: center;
  letter-spacing: 0.05em;
}

.slogan {
  text-align: center;
  color: var(--ink-2);
  font-family: var(--font-serif-zh);
  margin: var(--space-2) 0 var(--space-6);
  font-size: 0.95rem;
}

.field {
  margin-bottom: var(--space-4);
}

.field label {
  display: block;
  font-size: 0.85rem;
  color: var(--ink-2);
  margin-bottom: var(--space-2);
}

.field input {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid var(--line);
  border-radius: var(--radius-sm);
  background: var(--bg);
  color: var(--ink);
  font-size: 0.95rem;
  transition: border-color var(--transition-fast), box-shadow var(--transition-fast);
}

.field input:focus {
  outline: none;
  border-color: var(--accent);
  box-shadow: 0 0 0 3px var(--bg-hover);
}

.error-msg {
  color: var(--danger);
  font-size: 0.85rem;
  margin: 0 0 var(--space-3);
}

.submit-btn {
  width: 100%;
  padding: 11px;
  border: none;
  border-radius: var(--radius-sm);
  background: var(--accent);
  color: #fff;
  font-size: 1rem;
  letter-spacing: 0.3em;
  cursor: pointer;
  transition: background var(--transition-fast);
}

.submit-btn:hover:not(:disabled) {
  background: var(--accent-hover);
}

.submit-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.switch-tip {
  text-align: center;
  margin-top: var(--space-5);
  font-size: 0.85rem;
  color: var(--ink-2);
}
</style>
