import axios, { type AxiosInstance, type AxiosRequestConfig, type AxiosResponse } from 'axios'
import { ElMessage } from 'element-plus'
import type { ApiResponse } from '@/types'

/**
 * 创建axios实例
 */
const service: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '',
  timeout: 15000,
  headers: {
    'Content-Type': 'application/json;charset=UTF-8'
  },
  // 确保响应使用UTF-8编码
  responseType: 'json',
  responseCharset: 'utf8'
})

/**
 * 请求拦截器
 */
service.interceptors.request.use(
  (config) => {
    // 从localStorage获取token
    const token = localStorage.getItem('token')
    if (token) {
      // 设置JWT标准认证头
      config.headers['Authorization'] = `Bearer ${token}`
      // 同时也设置X-User-Id头，兼容现有接口
      // config.headers['X-User-Id'] = token
    }
    return config
  },
  (error) => {
    console.error('请求错误:', error)
    return Promise.reject(error)
  }
)

/**
 * 响应拦截器
 */
service.interceptors.response.use(
  (response: AxiosResponse<ApiResponse>) => {
    const { code, message, data } = response.data

    // 成功响应
    if (code === 200 || code === 0) {
      return data
    }

    // 业务错误
    ElMessage.error(message || '请求失败')
    return Promise.reject(new Error(message || '请求失败'))
  },
  (error) => {
    console.error('响应错误:', error)

    // 网络错误
    if (error.message.includes('Network Error')) {
      ElMessage.error('网络错误，请检查网络连接')
      return Promise.reject(error)
    }

    // 超时错误
    if (error.message.includes('timeout')) {
      ElMessage.error('请求超时，请重试')
      return Promise.reject(error)
    }

    // 401 未认证
    if (error.response?.status === 401) {
      ElMessage.error('登录已过期，请重新登录')
      localStorage.removeItem('token')
      window.location.href = '/login'
      return Promise.reject(error)
    }

    // 403 无权限
    if (error.response?.status === 403) {
      ElMessage.error('无权限访问')
      return Promise.reject(error)
    }

    // 404 资源不存在
    if (error.response?.status === 404) {
      ElMessage.error('请求的资源不存在')
      return Promise.reject(error)
    }

    // 500 服务器错误
    if (error.response?.status === 500) {
      ElMessage.error('服务器错误，请稍后重试')
      return Promise.reject(error)
    }

    // 其他错误
    const message = error.response?.data?.message || error.message || '请求失败'
    ElMessage.error(message)
    return Promise.reject(error)
  }
)

/**
 * 封装请求方法
 */
export const http = {
  get<T = any>(url: string, config?: AxiosRequestConfig): Promise<T> {
    return service.get(url, config)
  },

  post<T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<T> {
    return service.post(url, data, config)
  },

  put<T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<T> {
    return service.put(url, data, config)
  },

  delete<T = any>(url: string, config?: AxiosRequestConfig): Promise<T> {
    return service.delete(url, config)
  }
}

export default service
