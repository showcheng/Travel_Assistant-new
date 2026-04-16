<template>
  <div v-loading="userStore.loading" class="profile-container">
    <el-page-header @back="goBack" title="返回">
      <template #content>
        <span class="page-title">个人中心</span>
      </template>
    </el-page-header>

    <div v-if="userStore.currentUser" class="profile-content">
      <!-- 用户信息卡片 -->
      <el-card class="user-card">
        <div class="user-info">
          <el-avatar :size="100" :src="userStore.currentUser.avatar">
            {{ userStore.currentUser.phone?.slice(-2) }}
          </el-avatar>
          <div class="user-details">
            <h2 class="user-phone">{{ userStore.currentUser.phone }}</h2>
            <p v-if="userStore.currentUser.nickname" class="user-nickname">
              昵称：{{ userStore.currentUser.nickname }}
            </p>
            <p class="user-id">用户ID：{{ userStore.currentUser.id }}</p>
          </div>
        </div>
      </el-card>

      <!-- 账户信息 -->
      <el-card class="account-card">
        <template #header>
          <div class="card-header">
            <span>账户信息</span>
          </div>
        </template>

        <el-descriptions :column="1" border>
          <el-descriptions-item label="手机号">
            {{ userStore.currentUser.phone }}
          </el-descriptions-item>
          <el-descriptions-item label="用户ID">
            {{ userStore.currentUser.id }}
          </el-descriptions-item>
          <el-descriptions-item label="注册时间">
            {{ formatDateTime(userStore.currentUser.createTime) }}
          </el-descriptions-item>
          <el-descriptions-item label="账户状态">
            <el-tag :type="userStore.currentUser.status === 1 ? 'success' : 'danger'">
              {{ userStore.currentUser.status === 1 ? '正常' : '已禁用' }}
            </el-tag>
          </el-descriptions-item>
        </el-descriptions>
      </el-card>

      <!-- 快捷操作 -->
      <el-card class="actions-card">
        <template #header>
          <div class="card-header">
            <span>快捷操作</span>
          </div>
        </template>

        <div class="action-buttons">
          <el-button type="primary" @click="handleEditProfile">
            <el-icon><Edit /></el-icon>
            编辑资料
          </el-button>
          <el-button @click="handleChangePassword">
            <el-icon><Lock /></el-icon>
            修改密码
          </el-button>
          <el-button type="danger" @click="handleLogout">
            <el-icon><SwitchButton /></el-icon>
            退出登录
          </el-button>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { formatDateTime } from '@/utils'

const router = useRouter()
const userStore = useUserStore()

// 返回上一页
const goBack = () => {
  router.push('/products')
}

// 编辑资料
const handleEditProfile = () => {
  ElMessage.info('编辑资料功能开发中')
}

// 修改密码
const handleChangePassword = () => {
  ElMessage.info('修改密码功能开发中')
}

// 退出登录
const handleLogout = async () => {
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
}

// 初始化
onMounted(async () => {
  if (!userStore.currentUser) {
    await userStore.getCurrentUser()
  }
})
</script>

<style scoped>
.profile-container {
  padding: 20px;
  max-width: 800px;
  margin: 0 auto;
}

.page-title {
  font-size: 20px;
  font-weight: 600;
  color: #303133;
}

.profile-content {
  margin-top: 20px;
}

.user-card {
  margin-bottom: 20px;
}

.user-info {
  display: flex;
  gap: 20px;
  align-items: center;
}

.user-details {
  flex: 1;
}

.user-phone {
  font-size: 24px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 8px;
}

.user-nickname {
  font-size: 16px;
  color: #606266;
  margin-bottom: 4px;
}

.user-id {
  font-size: 14px;
  color: #909399;
  margin: 0;
}

.account-card {
  margin-bottom: 20px;
}

.card-header {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.action-buttons {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.action-buttons .el-button {
  width: 100%;
}
</style>
