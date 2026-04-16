/**
 * 通用类型定义
 */

/** API响应结构 */
export interface ApiResponse<T = any> {
  code: number
  message: string
  data: T
}

/** 分页参数 */
export interface PageParams {
  page: number
  size: number
  [key: string]: any
}

/** 分页响应 */
export interface PageResponse<T> {
  records: T[]
  total: number
  size: number
  current: number
  pages: number
}

/** 用户信息 */
export interface User {
  id: number
  phone: string
  nickname?: string
  avatarUrl?: string
  status: number
  createTime: string
}

/** 登录请求 */
export interface LoginRequest {
  phone: string
  password: string
  captchaCode?: string
}

/** 登录响应 - 匹配后端返回格式 */
export interface LoginResponse {
  accessToken: string
  refreshToken: string
  userId: number
  phone: string
  nickname: string
  avatarUrl?: string
  expiresIn: number
}

/** 商品信息 */
export interface Product {
  id: number
  name: string
  description: string
  type: string
  originalPrice: number
  currentPrice: number
  stock: number
  imageUrl?: string
  status: number
  scenicSpotId?: number
  createTime: string
  updateTime: string
}

/** 订单明细项 */
export interface OrderItem {
  id: number
  orderId: number
  productId: number
  productName: string
  productImage?: string
  quantity: number
  price: number
  totalPrice: number
  deleted: number
}

/** 订单信息 */
export interface Order {
  id: number
  orderNo: string
  userId: number
  totalAmount: number
  payAmount: number
  status: number
  deleted: number
  totalPrice: number
  createTime?: string
  updateTime?: string
  payTime?: string
  // 兼容旧版本
  productId?: number
  productName?: string
  productImage?: string
  quantity?: number
  remark?: string
  // 新增：订单明细列表
  items?: OrderItem[]
}

/** 订单明细创建请求 */
export interface OrderItemCreateRequest {
  productId: number
  productName?: string
  productImage?: string
  quantity: number
  price: number
}

/** 订单创建请求 */
export interface CreateOrderRequest {
  items: OrderItemCreateRequest[]
  remark?: string
  // 兼容旧版本
  productId?: number
  quantity?: number
}

/** 路由元信息 */
export interface RouteMetaCustom {
  title?: string
  requiresAuth?: boolean
  icon?: string
}
