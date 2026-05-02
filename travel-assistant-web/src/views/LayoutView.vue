<template>
  <el-container class="layout-container">
    <!-- 顶部导航栏 -->
    <el-header class="layout-header">
      <div class="header-content">
        <div class="logo">
          <el-icon :size="28" color="#409eff"><Location /></el-icon>
          <span class="logo-text">智慧旅游助手</span>
        </div>

        <el-menu
          :default-active="activeMenu"
          router
          mode="horizontal"
          :ellipsis="false"
          class="header-menu"
        >
          <el-menu-item index="/products">
            <el-icon><ShoppingBag /></el-icon>
            <span>商品列表</span>
          </el-menu-item>
          <el-menu-item index="/orders">
            <el-icon><Document /></el-icon>
            <span>我的订单</span>
          </el-menu-item>
          <el-menu-item index="/ai-chat">
            <el-icon><ChatDotRound /></el-icon>
            <span>AI助手</span>
          </el-menu-item>
          <el-menu-item index="/knowledge-base">
            <el-icon><Reading /></el-icon>
            <span>知识库</span>
          </el-menu-item>
          <el-menu-item index="/order-statistics">
            <el-icon><DataAnalysis /></el-icon>
            <span>订单统计</span>
          </el-menu-item>
          <el-menu-item index="/profile">
            <el-icon><User /></el-icon>
            <span>个人中心</span>
          </el-menu-item>
        </el-menu>

        <div class="header-actions">
          <el-dropdown @command="handleCommand">
            <span class="user-info">
              <el-avatar :size="32" :src="userStore.currentUser?.avatar">
                {{ userStore.currentUser?.phone?.slice(-2) }}
              </el-avatar>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">
                  <el-icon><User /></el-icon>
                  个人中心
                </el-dropdown-item>
                <el-dropdown-item divided command="logout">
                  <el-icon><SwitchButton /></el-icon>
                  退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>
    </el-header>

    <!-- 主要内容区域 -->
    <el-main class="layout-main">
      <router-view v-slot="{ Component }">
        <transition name="fade" mode="out-in">
          <component :is="Component" />
        </transition>
      </router-view>
    </el-main>

    <!-- 底部信息栏 -->
    <el-footer class="layout-footer">
      <p>&copy; 2024 智慧旅游助手. All rights reserved.</p>
    </el-footer>
  </el-container>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox, ElNotification } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { websocketManager } from '@/utils/websocket'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

// 当前激活的菜单
const activeMenu = computed(() => route.path)

// 处理下拉菜单命令
const handleCommand = async (command: string) => {
  switch (command) {
    case 'profile':
      router.push('/profile')
      break
    case 'logout':
      try {
        await ElMessageBox.confirm('确定要退出登录吗？', '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        })
        userStore.logout()
        ElMessage.success('退出登录成功')
        router.push('/login')
      } catch {
        // 用户取消
      }
      break
  }
}

// WebSocket消息处理
const handleWebSocketMessage = (data: any) => {
  if (data.type === 'order') {
    ElNotification({
      title: '订单状态变更',
      message: `订单 ${data.orderNo} 已${data.statusText}`,
      type: 'info',
      duration: 5000,
      onClick: () => {
        router.push(`/orders/${data.orderId}`)
      }
    })
  }
}

// 初始化
onMounted(async () => {
  if (userStore.isLoggedIn && !userStore.currentUser) {
    await userStore.getCurrentUser()

    // 连接WebSocket
    if (userStore.currentUser?.id) {
      websocketManager.connect(userStore.currentUser.id)
      websocketManager.onMessage(handleWebSocketMessage)
    }
  }
})

// 清理
onUnmounted(() => {
  websocketManager.disconnect()
})
</script>

<style scoped>
.layout-container {
  min-height: 100vh;
}

.layout-header {
  background: white;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  padding: 0;
}

.header-content {
  display: flex;
  align-items: center;
  max-width: 1400px;
  margin: 0 auto;
  height: 100%;
  padding: 0 20px;
}

.logo {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 20px;
  font-weight: bold;
  color: #409eff;
  margin-right: 40px;
}

.logo-text {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.header-menu {
  flex: 1;
  border: none;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 16px;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 4px;
  transition: background-color 0.3s;
}

.user-info:hover {
  background-color: #f5f7fa;
}

.layout-main {
  max-width: 1400px;
  margin: 0 auto;
  padding: 20px;
  min-height: calc(100vh - 120px);
}

.layout-footer {
  background: #f5f7fa;
  text-align: center;
  color: #909399;
  border-top: 1px solid #ebeef5;
}

/* 页面切换动画 */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
