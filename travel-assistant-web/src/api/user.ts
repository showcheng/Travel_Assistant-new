import { http } from '@/utils/request'
import type { LoginRequest, LoginResponse, User } from '@/types'

/**
 * 用户API
 */
export const userApi = {
  /**
   * 手机号登录
   */
  login(data: LoginRequest) {
    return http.post<LoginResponse>('/api/auth/login', data)
  },

  /**
   * 发送短信验证码
   */
  sendSmsCode(phone: string) {
    return http.post<void>('/api/auth/sms/send', { phone })
  },

  /**
   * 获取当前用户信息
   */
  getCurrentUser() {
    return http.get<User>('/api/user/info')
  },

  /**
   * 退出登录
   */
  logout() {
    return http.post<void>('/api/auth/logout')
  }
}
