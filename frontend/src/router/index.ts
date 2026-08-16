import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '@/views/HomeView.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      name: 'home',
      component: HomeView,
    },
    // 后续步骤注册：/login /register /shelf /reading/:id /profile /vocab /favorites /recent /admin/*
  ],
})

export default router
