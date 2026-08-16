<template>
  <div class="profile page-container">
    <!-- 资料卡片 -->
    <div class="profile-card">
      <div class="avatar-wrap">
        <img v-if="form.avatarUrl" :src="form.avatarUrl" class="avatar" alt="头像" />
        <span v-else class="avatar avatar-fallback">
          {{ (form.nickname || userStore.userInfo?.username || 'R').slice(0, 1).toUpperCase() }}
        </span>
        <button class="avatar-edit" @click="editing = true">更换头像</button>
      </div>
      <div class="profile-info">
        <h2 class="profile-name">{{ form.nickname || userStore.userInfo?.username }}</h2>
        <p class="profile-username">@{{ userStore.userInfo?.username }}</p>
        <button
          v-if="userStore.isAdmin() && isDesktop"
          class="admin-btn"
          @click="router.push('/admin/articles')"
        >
          🛠 进入管理后台
        </button>
      </div>
      <button class="edit-btn" @click="editing = true">编辑资料</button>
    </div>

    <!-- 统计卡 -->
    <div class="stats">
      <div class="stat">
        <span class="stat-num">{{ userStore.userInfo?.completedCount ?? 0 }}</span>
        <span class="stat-label">精读完成</span>
      </div>
      <div class="stat">
        <span class="stat-num">{{ userStore.userInfo?.readingCount ?? 0 }}</span>
        <span class="stat-label">进行中</span>
      </div>
      <div class="stat">
        <span class="stat-num">{{ userStore.userInfo?.totalProgress ?? 0 }}%</span>
        <span class="stat-label">平均进度</span>
      </div>
    </div>

    <!-- 学习数据入口 -->
    <div class="entries">
      <router-link to="/recent" class="entry">
        <span class="entry-icon">📖</span>
        <span class="entry-name">近期阅读</span>
        <span class="entry-arrow">→</span>
      </router-link>
      <router-link to="/vocab" class="entry">
        <span class="entry-icon">📝</span>
        <span class="entry-name">生词本</span>
        <span class="entry-arrow">→</span>
      </router-link>
      <router-link to="/favorites" class="entry">
        <span class="entry-icon">❤️</span>
        <span class="entry-name">例句收藏</span>
        <span class="entry-arrow">→</span>
      </router-link>
      <button class="entry" @click="changePasswordOpen = true">
        <span class="entry-icon">🔒</span>
        <span class="entry-name">修改密码</span>
        <span class="entry-arrow">→</span>
      </button>
    </div>

    <!-- 编辑资料弹窗 -->
    <div v-if="editing" class="modal-mask" @click.self="editing = false">
      <div class="modal">
        <h3 class="modal-title">编辑资料</h3>
        <div class="field">
          <label>昵称</label>
          <input v-model.trim="form.nickname" type="text" maxlength="20" placeholder="输入昵称" />
        </div>
        <div class="field">
          <label>头像 URL</label>
          <input v-model.trim="form.avatarUrl" type="url" placeholder="https://…" />
        </div>
        <p v-if="saveError" class="error-msg">{{ saveError }}</p>
        <div class="modal-actions">
          <button class="btn-ghost" @click="editing = false">取消</button>
          <button class="btn-primary" :disabled="saving" @click="saveProfile">
            {{ saving ? '保存中…' : '保存' }}
          </button>
        </div>
      </div>
    </div>

    <!-- 修改密码弹窗 -->
    <div v-if="changePasswordOpen" class="modal-mask" @click.self="closeChangePassword">
      <div class="modal">
        <h3 class="modal-title">
          {{ isForced ? '首次登录，请修改默认密码' : '修改密码' }}
        </h3>
        <div class="field">
          <label>当前密码</label>
          <input v-model="pwdForm.oldPassword" type="password" autocomplete="current-password" />
        </div>
        <div class="field">
          <label>新密码（6-32 位）</label>
          <input v-model="pwdForm.newPassword" type="password" autocomplete="new-password" />
        </div>
        <p v-if="pwdError" class="error-msg">{{ pwdError }}</p>
        <div class="modal-actions">
          <button v-if="!isForced" class="btn-ghost" @click="changePasswordOpen = false">取消</button>
          <button class="btn-primary" :disabled="pwdSaving" @click="savePassword">
            {{ pwdSaving ? '提交中…' : '确认修改' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { authApi } from '@/api/auth'
import { userApi } from '@/api/user'
import { useUserStore } from '@/stores/user'
import { isDesktop } from '@/utils/device'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const editing = ref(false)
const saving = ref(false)
const saveError = ref('')
const changePasswordOpen = ref(false)
const pwdSaving = ref(false)
const pwdError = ref('')

const form = reactive({ nickname: '', avatarUrl: '' })
const pwdForm = reactive({ oldPassword: '', newPassword: '' })

/** 预置管理员首登强制改密（登录后跳转携带 changePassword=1） */
const isForced = ref(route.query.changePassword === '1')

function closeChangePassword() {
  if (!isForced.value) changePasswordOpen.value = false
}

async function saveProfile() {
  saving.value = true
  saveError.value = ''
  try {
    const updated = await userApi.updateProfile({
      nickname: form.nickname || undefined,
      avatarUrl: form.avatarUrl || undefined,
    })
    userStore.setUser(updated)
    editing.value = false
  } catch (e) {
    saveError.value = (e as Error).message || '保存失败'
  } finally {
    saving.value = false
  }
}

async function savePassword() {
  pwdSaving.value = true
  pwdError.value = ''
  try {
    await authApi.changePassword({
      oldPassword: pwdForm.oldPassword,
      newPassword: pwdForm.newPassword,
    })
    changePasswordOpen.value = false
    isForced.value = false
    // 刷新用户信息（mustChangePassword 标记清除）
    const me = await userStore.fetchMe()
    userStore.setUser(me)
    // 清掉 URL 中的强制改密标记
    router.replace({ query: {} })
  } catch (e) {
    pwdError.value = (e as Error).message || '修改失败'
  } finally {
    pwdSaving.value = false
  }
}

onMounted(async () => {
  if (!userStore.userInfo) {
    try {
      await userStore.fetchMe()
    } catch {
      return
    }
  }
  form.nickname = userStore.userInfo?.nickname ?? ''
  form.avatarUrl = userStore.userInfo?.avatarUrl ?? ''
  // 首次登录强制改密：自动弹出
  if (isForced.value || userStore.userInfo?.mustChangePassword) {
    changePasswordOpen.value = true
  }
})
</script>

<style scoped>
.profile {
  max-width: 720px;
  padding-top: var(--space-6);
  padding-bottom: var(--space-7);
  display: flex;
  flex-direction: column;
  gap: var(--space-5);
}

.profile-card {
  display: flex;
  align-items: center;
  gap: var(--space-4);
  background: var(--bg-card);
  border: 1px solid var(--line);
  border-radius: var(--radius-md);
  padding: var(--space-5);
}

.avatar-wrap {
  position: relative;
}

.avatar {
  width: 72px;
  height: 72px;
  border-radius: 50%;
  object-fit: cover;
}

.avatar-fallback {
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--accent);
  color: #fff;
  font-size: 1.8rem;
  font-weight: 600;
  font-family: var(--font-serif-en);
}

.avatar-edit {
  position: absolute;
  bottom: -2px;
  left: 50%;
  transform: translateX(-50%);
  padding: 1px 8px;
  border: none;
  border-radius: 999px;
  background: var(--accent);
  color: #fff;
  font-size: 0.68rem;
  cursor: pointer;
  white-space: nowrap;
}

.profile-info {
  flex: 1;
  min-width: 0;
}

.profile-name {
  font-family: var(--font-serif-zh);
  font-size: 1.3rem;
  color: var(--ink);
}

.profile-username {
  color: var(--ink-3);
  font-size: 0.85rem;
  margin-top: 2px;
}

.admin-btn {
  margin-top: var(--space-2);
  padding: 5px 12px;
  border: 1px solid var(--accent);
  border-radius: 999px;
  background: transparent;
  color: var(--accent);
  font-size: 0.78rem;
  cursor: pointer;
  transition: all var(--transition-fast);
}

.admin-btn:hover {
  background: var(--accent);
  color: #fff;
}

.edit-btn {
  padding: 6px 14px;
  border: 1px solid var(--line);
  border-radius: var(--radius-sm);
  background: transparent;
  color: var(--ink-2);
  font-size: 0.85rem;
  cursor: pointer;
  transition: all var(--transition-fast);
}

.edit-btn:hover {
  border-color: var(--accent);
  color: var(--accent);
}

.stats {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: var(--space-4);
}

.stat {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  background: var(--bg-card);
  border: 1px solid var(--line);
  border-radius: var(--radius-md);
  padding: var(--space-4);
}

.stat-num {
  font-family: var(--font-serif-en);
  font-size: 1.6rem;
  color: var(--accent);
  font-weight: 700;
}

.stat-label {
  color: var(--ink-2);
  font-size: 0.82rem;
}

.entries {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}

.entry {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  background: var(--bg-card);
  border: 1px solid var(--line);
  border-radius: var(--radius-sm);
  padding: var(--space-4);
  color: var(--ink);
  font-size: 0.95rem;
  cursor: pointer;
  transition: border-color var(--transition-fast);
}

.entry:hover {
  border-color: var(--accent);
}

.entry-icon {
  font-size: 1.1rem;
}

.entry-name {
  flex: 1;
  text-align: left;
}

.entry-arrow {
  color: var(--ink-3);
}

/* 弹窗 */
.modal-mask {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 200;
  padding: var(--space-4);
}

.modal {
  width: 100%;
  max-width: 400px;
  background: var(--bg-card);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow);
  padding: var(--space-5);
}

.modal-title {
  font-family: var(--font-serif-zh);
  font-size: 1.1rem;
  color: var(--ink);
  margin-bottom: var(--space-4);
}

.field {
  margin-bottom: var(--space-4);
}

.field label {
  display: block;
  font-size: 0.82rem;
  color: var(--ink-2);
  margin-bottom: var(--space-2);
}

.field input {
  width: 100%;
  padding: 9px 12px;
  border: 1px solid var(--line);
  border-radius: var(--radius-sm);
  background: var(--bg);
  color: var(--ink);
  font-size: 0.92rem;
}

.field input:focus {
  outline: none;
  border-color: var(--accent);
}

.error-msg {
  color: var(--danger);
  font-size: 0.82rem;
  margin-bottom: var(--space-3);
}

.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: var(--space-3);
}

.btn-ghost {
  padding: 8px 18px;
  border: 1px solid var(--line);
  border-radius: var(--radius-sm);
  background: transparent;
  color: var(--ink-2);
  font-size: 0.9rem;
  cursor: pointer;
}

.btn-primary {
  padding: 8px 18px;
  border: none;
  border-radius: var(--radius-sm);
  background: var(--accent);
  color: #fff;
  font-size: 0.9rem;
  cursor: pointer;
}

.btn-primary:hover:not(:disabled) {
  background: var(--accent-hover);
}

.btn-primary:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
</style>
