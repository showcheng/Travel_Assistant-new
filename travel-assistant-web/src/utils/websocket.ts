/**
 * WebSocket工具类
 * 用于接收订单状态变更通知
 */

class WebSocketManager {
  private ws: WebSocket | null = null
  private reconnectTimer: number | null = null
  private reconnectAttempts = 0
  private maxReconnectAttempts = 5
  private reconnectDelay = 3000 // 3秒后重连
  private userId: number | null = null
  private messageHandlers: ((data: any) => void)[] = []

  /**
   * 连接WebSocket
   */
  connect(userId: number): void {
    if (this.ws && this.ws.readyState === WebSocket.OPEN) {
      console.log('WebSocket已连接，无需重复连接')
      return
    }

    this.userId = userId
    const wsUrl = `ws://localhost:8088/ws/order/${userId}`

    try {
      this.ws = new WebSocket(wsUrl)

      this.ws.onopen = () => {
        console.log('WebSocket连接成功')
        this.reconnectAttempts = 0

        // 启动心跳
        this.startHeartbeat()
      }

      this.ws.onmessage = (event) => {
        console.log('收到WebSocket消息:', event.data)
        this.handleMessage(event.data)
      }

      this.ws.onerror = (error) => {
        console.error('WebSocket错误:', error)
      }

      this.ws.onclose = () => {
        console.log('WebSocket连接关闭')
        this.stopHeartbeat()

        // 尝试重连
        if (this.reconnectAttempts < this.maxReconnectAttempts) {
          this.reconnectAttempts++
          console.log(`WebSocket重连中... (${this.reconnectAttempts}/${this.maxReconnectAttempts})`)

          this.reconnectTimer = window.setTimeout(() => {
            this.connect(userId)
          }, this.reconnectDelay)
        } else {
          console.error('WebSocket重连次数已达上限')
        }
      }
    } catch (error) {
      console.error('WebSocket连接失败:', error)
    }
  }

  /**
   * 断开连接
   */
  disconnect(): void {
    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer)
      this.reconnectTimer = null
    }

    this.stopHeartbeat()

    if (this.ws) {
      this.ws.close()
      this.ws = null
    }
  }

  /**
   * 添加消息处理器
   */
  onMessage(handler: (data: any) => void): void {
    this.messageHandlers.push(handler)
  }

  /**
   * 移除消息处理器
   */
  offMessage(handler: (data: any) => void): void {
    const index = this.messageHandlers.indexOf(handler)
    if (index > -1) {
      this.messageHandlers.splice(index, 1)
    }
  }

  /**
   * 处理收到的消息
   */
  private handleMessage(data: string): void {
    try {
      // 处理心跳响应
      if (data === 'pong') {
        return
      }

      // 解析JSON消息
      const message = JSON.parse(data)

      // 调用所有消息处理器
      this.messageHandlers.forEach(handler => {
        try {
          handler(message)
        } catch (error) {
          console.error('消息处理器错误:', error)
        }
      })
    } catch (error) {
      console.error('解析WebSocket消息失败:', error)
    }
  }

  /**
   * 启动心跳
   */
  private heartbeatTimer: number | null = null

  private startHeartbeat(): void {
    this.stopHeartbeat()
    this.heartbeatTimer = window.setInterval(() => {
      if (this.ws && this.ws.readyState === WebSocket.OPEN) {
        this.ws.send('ping')
      }
    }, 30000) // 每30秒发送一次心跳
  }

  private stopHeartbeat(): void {
    if (this.heartbeatTimer) {
      clearInterval(this.heartbeatTimer)
      this.heartbeatTimer = null
    }
  }
}

// 导出单例
export const websocketManager = new WebSocketManager()