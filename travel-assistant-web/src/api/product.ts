import { http } from '@/utils/request'
import type { Product, PageParams, PageResponse } from '@/types'

/**
 * 商品API
 */
export const productApi = {
  /**
   * 分页查询商品
   */
  getProducts(params: PageParams) {
    return http.get<PageResponse<Product>>('/api/products', { params })
  },

  /**
   * 获取商品详情
   */
  getProductById(id: number) {
    return http.get<Product>(`/api/products/${id}`)
  }
}
