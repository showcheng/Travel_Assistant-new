import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { userApi } from '@/api/user'
import type { User } from '@/types'

export const useUserStore = defineStore('user', () => {
  // 状态
  const token = ref<string | null>(localStorage.getItem('token'))
  const currentUser = ref<User | null>(null)
  const loading = ref(false)

  // 计算属性
  const isLoggedIn = computed(() => !!token.value)
  const userId = computed(() => currentUser.value?.id)

  /**
   * 登录
   */
  async function login(phone: string, password: string) {
    loading.value = true
    try {
      const response = await userApi.login({ phone, password })

      // 后端返回的数据格式
      // {
      //   "code": 200,
      //   "data": {
      //     "accessToken": "...",
      //     "refreshToken": "...",
      //     "userId": 2,
      //     "phone": "18081258306",
      //     "nickname": "测试用户",
      //     ...
      //   }
      // }

      if (response && response.accessToken) {
        token.value = response.accessToken
        currentUser.value = {
          id: response.userId,
          phone: response.phone,
          nickname: response.nickname,
          avatarUrl: response.avatarUrl
        }
        localStorage.setItem('token', response.accessToken)

        console.log('登录成功，用户信息:', currentUser.value)
        return true
      } else {
        console.error('登录响应格式异常:', response)
        return false
      }
    } catch (error) {
      console.error('登录失败:', error)
      return false
    } finally {
      loading.value = false
    }
  }

  /**
   * 获取当前用户信息
   */
  async function getCurrentUser() {
    if (!token.value) {
      return false
    }

    loading.value = true
    try {
      const user = await userApi.getCurrentUser()
      currentUser.value = user
      return true
    } catch (error) {
      console.error('获取用户信息失败:', error)
      logout()
      return false
    } finally {
      loading.value = false
    }
  }

  /**
   * 退出登录
   */
  function logout() {
    token.value = null
    currentUser.value = null
    localStorage.removeItem('token')
  }

  return {
    token,
    currentUser,
    loading,
    isLoggedIn,
    userId,
    login,
    getCurrentUser,
    logout
  }
})
