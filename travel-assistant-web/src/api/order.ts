import { http } from '@/utils/request'
import type { Order, CreateOrderRequest, OrderItem, PageParams, PageResponse } from '@/types'

/**
 * 订单API
 */
export const orderApi = {
  /**
   * 创建订单
   */
  createOrder(data: CreateOrderRequest) {
    return http.post<number>('http://localhost:8083/api/orders', data)
  },

  /**
   * 取消订单
   */
  cancelOrder(id: number) {
    return http.post<void>(`http://localhost:8083/api/orders/${id}/cancel`)
  },

  /**
   * 支付订单
   */
  payOrder(id: number) {
    return http.post<void>(`http://localhost:8083/api/orders/${id}/pay`)
  },

  /**
   * 完成订单
   */
  completeOrder(id: number) {
    return http.post<void>(`http://localhost:8083/api/orders/${id}/complete`)
  },

  /**
   * 申请退款
   */
  refundOrder(id: number) {
    return http.post<void>(`http://localhost:8083/api/orders/${id}/refund`)
  },

  /**
   * 获取订单详情
   */
  getOrderById(id: number) {
    return http.get<Order>(`http://localhost:8083/api/orders/${id}`)
  },

  /**
   * 分页查询用户订单
   */
  getUserOrders(params: PageParams) {
    return http.get<PageResponse<Order>>('http://localhost:8083/api/orders', { params })
  },

  /**
   * 搜索订单
   */
  searchOrders(params: PageParams & { keyword: string }) {
    return http.get<PageResponse<Order>>('http://localhost:8083/api/orders/search', { params })
  },

  /**
   * 获取订单的所有明细
   */
  getOrderItems(orderId: number) {
    return http.get<OrderItem[]>(`http://localhost:8083/api/order-items/order/${orderId}`)
  },

  /**
   * 分页查询订单明细
   */
  getOrderItemsByPage(orderId: number, params: PageParams) {
    return http.get<PageResponse<OrderItem>>(`http://localhost:8083/api/order-items/order/${orderId}/page`, { params })
  },

  /**
   * 删除订单明细
   */
  deleteOrderItem(id: number) {
    return http.delete<void>(`http://localhost:8083/api/order-items/${id}`)
  },

  /**
   * 获取订单统计数据
   */
  getOrderStatistics() {
    return http.get<OrderStatistics>('http://localhost:8083/api/orders/statistics')
  },

  /**
   * 导出订单数据
   */
  exportOrders() {
    return `http://localhost:8083/api/orders/export`
  }
}

/**
 * 订单统计数据接口
 */
export interface OrderStatistics {
  totalOrders: number
  totalAmount: number
  statusCount: Record<string, number>
  statusAmount: Record<string, number>
  todayOrders: number
  todayAmount: number
  monthOrders: number
  monthAmount: number
  averageAmount: number
}
