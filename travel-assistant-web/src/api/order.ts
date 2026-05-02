import { http } from '@/utils/request'
import type { Order, CreateOrderRequest, OrderItem, PageParams, PageResponse } from '@/types'

/**
 * 订单API
 */
export const orderApi = {
  createOrder(data: CreateOrderRequest) {
    return http.post<number>('/api/orders', data)
  },

  cancelOrder(id: number) {
    return http.post<void>(`/api/orders/${id}/cancel`)
  },

  payOrder(id: number) {
    return http.post<void>(`/api/orders/${id}/pay`)
  },

  completeOrder(id: number) {
    return http.post<void>(`/api/orders/${id}/complete`)
  },

  refundOrder(id: number) {
    return http.post<void>(`/api/orders/${id}/refund`)
  },

  getOrderById(id: number) {
    return http.get<Order>(`/api/orders/${id}`)
  },

  getUserOrders(params: PageParams) {
    return http.get<PageResponse<Order>>('/api/orders', { params })
  },

  searchOrders(params: PageParams & { keyword: string }) {
    return http.get<PageResponse<Order>>('/api/orders/search', { params })
  },

  getOrderItems(orderId: number) {
    return http.get<OrderItem[]>(`/api/order-items/order/${orderId}`)
  },

  getOrderItemsByPage(orderId: number, params: PageParams) {
    return http.get<PageResponse<OrderItem>>(`/api/order-items/order/${orderId}/page`, { params })
  },

  deleteOrderItem(id: number) {
    return http.delete<void>(`/api/order-items/${id}`)
  },

  getOrderStatistics() {
    return http.get<OrderStatistics>('/api/orders/statistics')
  },

  exportOrders() {
    return '/api/orders/export'
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
