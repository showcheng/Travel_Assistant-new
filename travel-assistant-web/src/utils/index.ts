/**
 * 工具函数
 */

/**
 * 格式化日期时间
 */
export function formatDateTime(date: string | Date | null | undefined): string {
  if (!date) {
    return '-'
  }

  const d = typeof date === 'string' ? new Date(date) : date

  // 检查日期是否有效
  if (isNaN(d.getTime())) {
    return '-'
  }

  const year = d.getFullYear()
  const month = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  const hours = String(d.getHours()).padStart(2, '0')
  const minutes = String(d.getMinutes()).padStart(2, '0')
  const seconds = String(d.getSeconds()).padStart(2, '0')

  return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`
}

/**
 * 格式化日期
 */
export function formatDate(date: string | Date): string {
  const d = typeof date === 'string' ? new Date(date) : date
  const year = d.getFullYear()
  const month = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')

  return `${year}-${month}-${day}`
}

/**
 * 格式化金额
 */
export function formatMoney(amount: number): string {
  return `¥${amount.toFixed(2)}`
}

/**
 * 获取订单状态文本
 */
export function getOrderStatusText(status: number): string {
  const statusMap: Record<number, string> = {
    0: '待支付',
    1: '已支付',
    2: '已完成',
    3: '已退款',
    4: '已取消'
  }
  return statusMap[status] || '未知状态'
}

/**
 * 获取订单状态颜色
 */
export function getOrderStatusColor(status: number): string {
  const colorMap: Record<number, string> = {
    0: 'warning',  // 待支付
    1: 'success',  // 已支付
    2: 'info',     // 已取消
    3: 'danger',   // 已退款
    4: 'info'      // 已完成
  }
  return colorMap[status] || 'info'
}

/**
 * 获取商品状态文本
 */
export function getProductStatusText(status: number): string {
  const statusMap: Record<number, string> = {
    0: '下架',
    1: '上架'
  }
  return statusMap[status] || '未知状态'
}

/**
 * 防抖函数
 */
export function debounce<T extends (...args: any[]) => any>(
  fn: T,
  delay: number
): (...args: Parameters<T>) => void {
  let timeoutId: ReturnType<typeof setTimeout> | null = null

  return function(this: any, ...args: Parameters<T>) {
    if (timeoutId) {
      clearTimeout(timeoutId)
    }

    timeoutId = setTimeout(() => {
      fn.apply(this, args)
    }, delay)
  }
}

/**
 * 节流函数
 */
export function throttle<T extends (...args: any[]) => any>(
  fn: T,
  delay: number
): (...args: Parameters<T>) => void {
  let lastTime = 0

  return function(this: any, ...args: Parameters<T>) {
    const now = Date.now()

    if (now - lastTime >= delay) {
      lastTime = now
      fn.apply(this, args)
    }
  }
}
