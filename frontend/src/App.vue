<template>
  <div class="app-shell">
    <AppHeader v-if="showHeader" />
    <main class="app-main">
      <router-view />
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import AppHeader from '@/components/common/AppHeader.vue'

const route = useRoute()

// 登录/注册页为沉浸式卡片页，不显示全局头部
const showHeader = computed(() => !['login', 'register'].includes(String(route.name)))
</script>

<style scoped>
.app-shell {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.app-main {
  flex: 1;
  display: flex;
  flex-direction: column;
}
</style>
