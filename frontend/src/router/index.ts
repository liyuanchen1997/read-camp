import { createRouter, createWebHistory } from 'vue-router'
import { authGuard } from './guard'

// 路由 meta 类型扩展（requiresAuth / requiresAdmin）
declare module 'vue-router' {
  interface RouteMeta {
    requiresAuth?: boolean
    requiresAdmin?: boolean
    title?: string
  }
}

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      name: 'home',
      component: () => import('@/views/ShelfView.vue'),
      meta: { title: '书架' }, // 书架公开浏览，无需登录
    },
    {
      path: '/login',
      name: 'login',
      component: () => import('@/views/LoginView.vue'),
      meta: { title: '登录' },
    },
    {
      path: '/register',
      name: 'register',
      component: () => import('@/views/RegisterView.vue'),
      meta: { title: '注册' },
    },
    {
      path: '/reading/:id',
      name: 'reading',
      component: () => import('@/views/ReadingView.vue'),
      meta: { requiresAuth: true, title: '精读' },
    },
    {
      path: '/profile',
      name: 'profile',
      component: () => import('@/views/ProfileView.vue'),
      meta: { requiresAuth: true, title: '我的' },
    },
    {
      path: '/vocab',
      name: 'vocab',
      component: () => import('@/views/VocabView.vue'),
      meta: { requiresAuth: true, title: '生词本' },
    },
    {
      path: '/favorites',
      name: 'favorites',
      component: () => import('@/views/FavoritesView.vue'),
      meta: { requiresAuth: true, title: '例句收藏' },
    },
    {
      path: '/recent',
      name: 'recent',
      component: () => import('@/views/RecentReadingView.vue'),
      meta: { requiresAuth: true, title: '近期阅读' },
    },
    {
      path: '/admin',
      component: () => import('@/views/admin/AdminLayoutView.vue'),
      meta: { requiresAuth: true, requiresAdmin: true, title: '管理后台' },
      children: [
        {
          path: '',
          redirect: '/admin/articles',
        },
        {
          path: 'articles',
          name: 'admin-articles',
          component: () => import('@/views/admin/AdminArticlesView.vue'),
          meta: { title: '文章管理' },
        },
        {
          path: 'stats',
          name: 'admin-stats',
          component: () => import('@/views/admin/AdminStatsView.vue'),
          meta: { title: '仪表盘' },
        },
      ],
    },
    {
      path: '/:pathMatch(.*)*',
      name: 'not-found',
      component: () => import('@/views/NotFoundView.vue'),
      meta: { title: '页面不存在' },
    },
  ],
})

router.beforeEach(authGuard)

export default router
