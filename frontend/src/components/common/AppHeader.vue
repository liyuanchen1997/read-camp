<template>
  <header class="app-header">
    <div class="header-inner">
      <router-link to="/" class="logo">英语精读训练营</router-link>

      <nav class="nav">
        <router-link to="/" class="nav-link" active-class="active">书架</router-link>
        <router-link
          v-if="userStore.isAdmin() && isDesktop"
          to="/admin/articles"
          class="nav-link"
        >
          管理后台
        </router-link>
      </nav>

      <div class="actions">
        <!-- 主题切换 -->
        <button class="icon-btn" :title="themeStore.theme === 'dark' ? '切换亮色' : '切换暗色'"
                @click="themeStore.toggle()" :aria-label="'切换主题'">
          <svg v-if="themeStore.theme === 'dark'" width="18" height="18" viewBox="0 0 24 24" fill="none"
               stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <circle cx="12" cy="12" r="4" />
            <path d="M12 2v2m0 16v2M4.9 4.9l1.4 1.4m11.4 11.4 1.4 1.4M2 12h2m16 0h2M4.9 19.1l1.4-1.4m11.4-11.4 1.4-1.4" />
          </svg>
          <svg v-else width="18" height="18" viewBox="0 0 24 24" fill="none"
               stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M21 12.8A9 9 0 1 1 11.2 3a7 7 0 0 0 9.8 9.8Z" />
          </svg>
        </button>

        <!-- 用户菜单 -->
        <template v-if="userStore.isLoggedIn()">
          <div v-if="userStore.userInfo" class="user-menu" ref="menuRef">
            <button class="user-trigger" @click="menuOpen = !menuOpen">
              <span v-if="userStore.userInfo.avatarUrl" class="avatar-img">
                <img :src="userStore.userInfo.avatarUrl" alt="头像" />
              </span>
              <span v-else class="avatar-fallback">
                {{ (userStore.userInfo.nickname || userStore.userInfo.username).slice(0, 1).toUpperCase() }}
              </span>
              <span class="nickname">{{ userStore.userInfo.nickname }}</span>
              <span class="caret" :class="{ open: menuOpen }">▾</span>
            </button>

            <transition name="dropdown">
              <div v-if="menuOpen" class="dropdown">
                <router-link to="/profile" class="menu-item" @click="menuOpen = false">
                  个人中心
                </router-link>
                <router-link v-if="userStore.isAdmin() && isDesktop" to="/admin/articles"
                             class="menu-item" @click="menuOpen = false">
                  管理后台
                </router-link>
                <button class="menu-item danger" @click="onLogout">退出登录</button>
              </div>
            </transition>
          </div>
        </template>
        <template v-else>
          <router-link to="/login" class="login-link">登录</router-link>
          <router-link to="/register" class="register-link">注册</router-link>
        </template>
      </div>
    </div>
  </header>
</template>

<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useThemeStore } from '@/stores/theme'
import { useUserStore } from '@/stores/user'
import { isDesktop } from '@/utils/device'

const userStore = useUserStore()
const themeStore = useThemeStore()
const router = useRouter()

const menuOpen = ref(false)
const menuRef = ref<HTMLElement | null>(null)

function onLogout() {
  userStore.logout()
  menuOpen.value = false
  router.push('/login')
}

function onDocClick(e: MouseEvent) {
  if (menuRef.value && !menuRef.value.contains(e.target as Node)) {
    menuOpen.value = false
  }
}

onMounted(() => document.addEventListener('click', onDocClick))
onBeforeUnmount(() => document.removeEventListener('click', onDocClick))
</script>

<style scoped>
.app-header {
  position: sticky;
  top: 0;
  z-index: 100;
  background: color-mix(in srgb, var(--bg) 88%, transparent);
  backdrop-filter: blur(8px);
  border-bottom: 1px solid var(--line);
}

.header-inner {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 var(--space-5);
  height: 60px;
  display: flex;
  align-items: center;
  gap: var(--space-5);
}

.logo {
  font-family: var(--font-serif-zh);
  font-size: 1.15rem;
  font-weight: 700;
  color: var(--accent);
  letter-spacing: 0.04em;
}

.nav {
  display: flex;
  gap: var(--space-4);
  flex: 1;
}

.nav-link {
  color: var(--ink-2);
  font-size: 0.95rem;
  padding: 4px 2px;
  border-bottom: 2px solid transparent;
  transition: color var(--transition-fast);
}

.nav-link:hover {
  color: var(--ink);
}

.nav-link.active {
  color: var(--accent);
  border-bottom-color: var(--accent);
}

.actions {
  display: flex;
  align-items: center;
  gap: var(--space-3);
}

.icon-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  border: 1px solid var(--line);
  border-radius: 50%;
  background: transparent;
  color: var(--ink-2);
  cursor: pointer;
  transition: color var(--transition-fast), border-color var(--transition-fast);
}

.icon-btn:hover {
  color: var(--accent);
  border-color: var(--accent);
}

/* 用户菜单 */
.user-menu {
  position: relative;
}

.user-trigger {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  border: none;
  background: transparent;
  cursor: pointer;
  padding: 4px 6px;
  border-radius: var(--radius-sm);
  color: var(--ink);
}

.user-trigger:hover {
  background: var(--bg-muted);
}

.avatar-img img,
.avatar-fallback {
  width: 30px;
  height: 30px;
  border-radius: 50%;
  object-fit: cover;
}

.avatar-fallback {
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--accent);
  color: #fff;
  font-size: 0.9rem;
  font-weight: 600;
}

.nickname {
  font-size: 0.9rem;
  max-width: 100px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.caret {
  font-size: 0.7rem;
  color: var(--ink-3);
  transition: transform var(--transition-fast);
}

.caret.open {
  transform: rotate(180deg);
}

.dropdown {
  position: absolute;
  right: 0;
  top: calc(100% + 6px);
  min-width: 140px;
  background: var(--bg-card);
  border: 1px solid var(--line);
  border-radius: var(--radius-sm);
  box-shadow: var(--shadow);
  padding: 4px;
  z-index: 110;
}

.menu-item {
  display: block;
  width: 100%;
  text-align: left;
  padding: 8px 12px;
  border: none;
  border-radius: 6px;
  background: transparent;
  color: var(--ink);
  font-size: 0.9rem;
  cursor: pointer;
  transition: background var(--transition-fast);
}

.menu-item:hover {
  background: var(--bg-hover);
}

.menu-item.danger {
  color: var(--danger);
}

/* 登录/注册链接（未登录） */
.login-link {
  color: var(--ink-2);
  font-size: 0.95rem;
}

.register-link {
  padding: 6px 16px;
  border-radius: var(--radius-sm);
  background: var(--accent);
  color: #fff;
  font-size: 0.9rem;
  transition: background var(--transition-fast);
}

.register-link:hover {
  background: var(--accent-hover);
  color: #fff;
}

/* 下拉动画 */
.dropdown-enter-active,
.dropdown-leave-active {
  transition: opacity var(--transition-fast), transform var(--transition-fast);
}

.dropdown-enter-from,
.dropdown-leave-to {
  opacity: 0;
  transform: translateY(-4px);
}

@media (max-width: 767px) {
  .nickname {
    display: none;
  }
}
</style>
