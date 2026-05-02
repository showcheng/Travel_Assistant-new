import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { useUserStore } from '@/stores/user'

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    redirect: '/products'
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/LoginView.vue'),
    meta: { title: '登录', requiresAuth: false }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/RegisterView.vue'),
    meta: { title: '注册', requiresAuth: false }
  },
  {
    path: '/',
    component: () => import('@/views/LayoutView.vue'),
    meta: { requiresAuth: true },
    children: [
      {
        path: 'products',
        name: 'Products',
        component: () => import('@/views/ProductListView.vue'),
        meta: { title: '商品列表', requiresAuth: true, icon: 'ShoppingBag' }
      },
      {
        path: 'products/:id',
        name: 'ProductDetail',
        component: () => import('@/views/ProductDetailView.vue'),
        meta: { title: '商品详情', requiresAuth: true }
      },
      {
        path: 'orders',
        name: 'Orders',
        component: () => import('@/views/OrderListView.vue'),
        meta: { title: '我的订单', requiresAuth: true, icon: 'Document' }
      },
      {
        path: 'order-statistics',
        name: 'OrderStatistics',
        component: () => import('@/views/OrderStatisticsView.vue'),
        meta: { title: '订单统计', requiresAuth: true, icon: 'DataAnalysis' }
      },
      {
        path: 'orders/:id',
        name: 'OrderDetail',
        component: () => import('@/views/OrderDetailView.vue'),
        meta: { title: '订单详情', requiresAuth: true }
      },
      {
        path: 'orders/create',
        name: 'CreateOrder',
        component: () => import('@/views/CreateOrderView.vue'),
        meta: { title: '创建订单', requiresAuth: true }
      },
      {
        path: 'payment/:id',
        name: 'Payment',
        component: () => import('@/views/PaymentView.vue'),
        meta: { title: '收银台', requiresAuth: true }
      },
      {
        path: 'profile',
        name: 'Profile',
        component: () => import('@/views/ProfileView.vue'),
        meta: { title: '个人中心', requiresAuth: true, icon: 'User' }
      },
      {
        path: 'ai-chat',
        name: 'AIChat',
        component: () => import('@/views/AIChatView.vue'),
        meta: { title: 'AI智能助手', requiresAuth: true, icon: 'ChatDotRound' }
      },
      {
        path: 'knowledge-base',
        name: 'KnowledgeBase',
        component: () => import('@/views/KnowledgeBaseView.vue'),
        meta: { title: '知识库管理', requiresAuth: true, icon: 'Reading' }
      }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/NotFoundView.vue'),
    meta: { title: '页面不存在' }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫
router.beforeEach((to, from, next) => {
  const userStore = useUserStore()
  const requiresAuth = to.meta.requiresAuth !== false

  // 设置页面标题
  if (to.meta.title) {
    document.title = `${to.meta.title} - 智慧旅游助手`
  }

  // 需要认证但未登录
  if (requiresAuth && !userStore.isLoggedIn) {
    next('/login')
    return
  }

  // 已登录访问登录页
  if (userStore.isLoggedIn && (to.path === '/login' || to.path === '/register')) {
    next('/products')
    return
  }

  next()
})

export default router
