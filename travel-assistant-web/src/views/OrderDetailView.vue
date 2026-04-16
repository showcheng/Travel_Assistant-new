<template>
  <div class="order-detail-container">
    <!-- 页面头部 -->
    <div class="page-header">
      <el-button @click="goBack" :icon="ArrowLeft">返回</el-button>
      <h1>订单详情</h1>
    </div>

    <div v-loading="loading" class="order-detail-content">
      <!-- 订单不存在 -->
      <el-empty v-if="!loading && !order" description="订单不存在" />

      <!-- 订单详情 -->
      <div v-else-if="order" class="detail-sections">
        <!-- 订单状态卡片 -->
        <el-card class="status-card">
          <div class="status-content">
            <div class="status-info">
              <el-tag :type="getOrderStatusColor(order.status)" size="large" class="status-tag">
                {{ getOrderStatusText(order.status) }}
              </el-tag>
              <div class="order-meta">
                <p class="order-no">订单号：{{ order.orderNo }}</p>
                <p class="create-time">创建时间：{{ formatDateTime(order.createTime) }}</p>
                <p v-if="order.payTime" class="pay-time">支付时间：{{ formatDateTime(order.payTime) }}</p>
              </div>
            </div>

            <!-- 订单进度 -->
            <div class="order-progress">
              <el-steps :active="getOrderProgressStep(order.status)" finish-status="success">
                <el-step title="待支付" />
                <el-step title="已支付" />
                <el-step title="已完成" />
              </el-steps>
            </div>
          </div>
        </el-card>

        <!-- 订单明细列表 -->
        <el-card class="items-card">
          <template #header>
            <div class="card-header">
              <span>商品清单</span>
              <span class="item-count">共 {{ getTotalItemCount() }} 件商品</span>
            </div>
          </template>

          <!-- 多商品订单 -->
          <div v-if="orderItems && orderItems.length > 0" class="order-items">
            <div
              v-for="item in orderItems"
              :key="item.id"
              class="order-item"
            >
              <el-row :gutter="16" align="middle">
                <el-col :span="3">
                  <div class="product-image">
                    <el-image
                      :src="item.productImage || '/placeholder.jpg'"
                      fit="cover"
                    >
                      <template #error>
                        <div class="image-error">
                          <el-icon><Picture /></el-icon>
                        </div>
                      </template>
                    </el-image>
                  </div>
                </el-col>

                <el-col :span="13">
                  <div class="product-info">
                    <h3 class="product-name">{{ item.productName }}</h3>
                    <p class="product-sku">商品ID：{{ item.productId }}</p>
                  </div>
                </el-col>

                <el-col :span="4">
                  <div class="product-quantity">
                    <span class="label">数量：</span>
                    <span class="value">× {{ item.quantity }}</span>
                  </div>
                </el-col>

                <el-col :span="4">
                  <div class="product-price">
                    <span class="label">小计：</span>
                    <span class="price">¥{{ item.totalPrice }}</span>
                  </div>
                </el-col>
              </el-row>
            </div>
          </div>

          <!-- 单商品订单（向后兼容） -->
          <div v-else-if="order.productName" class="order-items">
            <div class="order-item">
              <el-row :gutter="16" align="middle">
                <el-col :span="3">
                  <div class="product-image">
                    <el-image
                      :src="order.productImage || '/placeholder.jpg'"
                      fit="cover"
                    >
                      <template #error>
                        <div class="image-error">
                          <el-icon><Picture /></el-icon>
                        </div>
                      </template>
                    </el-image>
                  </div>
                </el-col>

                <el-col :span="13">
                  <div class="product-info">
                    <h3 class="product-name">{{ order.productName }}</h3>
                    <p class="product-sku">商品ID：{{ order.productId }}</p>
                  </div>
                </el-col>

                <el-col :span="4">
                  <div class="product-quantity">
                    <span class="label">数量：</span>
                    <span class="value">× {{ order.quantity }}</span>
                  </div>
                </el-col>

                <el-col :span="4">
                  <div class="product-price">
                    <span class="label">小计：</span>
                    <span class="price">¥{{ order.totalPrice }}</span>
                  </div>
                </el-col>
              </el-row>
            </div>
          </div>

          <!-- 订单总计 -->
          <div class="order-total">
            <el-row>
              <el-col :span="18" />
              <el-col :span="6">
                <div class="total-item">
                  <span class="label">商品总额：</span>
                  <span class="value">¥{{ order.totalAmount || order.totalPrice }}</span>
                </div>
                <div class="total-item">
                  <span class="label">应付金额：</span>
                  <span class="value total-price">¥{{ order.payAmount || order.totalPrice }}</span>
                </div>
              </el-col>
            </el-row>
          </div>
        </el-card>

        <!-- 订单信息 -->
        <el-card class="info-card">
          <template #header>
            <span>订单信息</span>
          </template>

          <el-descriptions :column="2" border>
            <el-descriptions-item label="订单号">
              {{ order.orderNo }}
            </el-descriptions-item>
            <el-descriptions-item label="订单状态">
              <el-tag :type="getOrderStatusColor(order.status)" size="small">
                {{ getOrderStatusText(order.status) }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="创建时间">
              {{ formatDateTime(order.createTime) }}
            </el-descriptions-item>
            <el-descriptions-item label="支付时间">
              {{ order.payTime ? formatDateTime(order.payTime) : '未支付' }}
            </el-descriptions-item>
            <el-descriptions-item label="商品数量" :span="2">
              {{ getTotalItemCount() }} 件
            </el-descriptions-item>
            <el-descriptions-item label="订单总额" :span="2">
              <span class="price-highlight">¥{{ order.totalAmount || order.totalPrice }}</span>
            </el-descriptions-item>
            <el-descriptions-item v-if="order.remark" label="备注" :span="2">
              {{ order.remark }}
            </el-descriptions-item>
          </el-descriptions>
        </el-card>

        <!-- 操作按钮 -->
        <div class="action-buttons">
          <el-button
            v-if="order.status === 0"
            type="primary"
            size="large"
            @click="handlePay"
          >
            立即支付
          </el-button>
          <el-button
            v-if="order.status === 0"
            type="danger"
            size="large"
            @click="handleCancel"
          >
            取消订单
          </el-button>
          <el-button
            v-if="order.status === 1"
            type="success"
            size="large"
            @click="handleComplete"
          >
            完成订单
          </el-button>
          <el-button
            v-if="order.status === 1"
            type="warning"
            size="large"
            @click="handleRefund"
          >
            申请退款
          </el-button>
          <el-button
            size="large"
            @click="goBack"
          >
            返回列表
          </el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, Picture } from '@element-plus/icons-vue'
import { orderApi } from '@/api/order'
import { formatDateTime, getOrderStatusText, getOrderStatusColor } from '@/utils'
import type { Order, OrderItem } from '@/types'

const route = useRoute()
const router = useRouter()

// 状态
const loading = ref(false)
const order = ref<Order | null>(null)
const orderItems = ref<OrderItem[]>([])

// 加载订单详情
const loadOrderDetail = async () => {
  const orderId = Number(route.params.id)
  if (!orderId) {
    ElMessage.error('订单ID无效')
    goBack()
    return
  }

  loading.value = true
  try {
    // 加载订单基本信息
    const orderData = await orderApi.getOrderById(orderId)
    order.value = orderData

    // 加载订单明细
    try {
      const items = await orderApi.getOrderItems(orderId)
      orderItems.value = items
    } catch (error) {
      console.error('加载订单明细失败:', error)
      // 如果加载明细失败，不影响订单详情显示
    }
  } catch (error: any) {
    console.error('加载订单详情失败:', error)
    ElMessage.error(error.message || '加载订单详情失败')
    goBack()
  } finally {
    loading.value = false
  }
}

// 获取订单进度步骤
const getOrderProgressStep = (status: number): number => {
  switch (status) {
    case 0: return 0 // 待支付
    case 1: return 1 // 已支付
    case 2: return 2 // 已完成
    case 3: return 0 // 已取消
    case 4: return 1 // 已退款
    default: return 0
  }
}

// 获取商品总数
const getTotalItemCount = (): number => {
  if (orderItems.value && orderItems.value.length > 0) {
    return orderItems.value.reduce((sum, item) => sum + item.quantity, 0)
  }
  return order.value?.quantity || 0
}

// 支付订单
const handlePay = () => {
  if (!order.value) return

  // 跳转到支付页面
  router.push(`/payment/${order.value.id}`)
}

// 取消订单
const handleCancel = async () => {
  if (!order.value) return

  try {
    await ElMessageBox.confirm('确定要取消该订单吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    await orderApi.cancelOrder(order.value.id)
    ElMessage.success('订单已取消')
    loadOrderDetail() // 重新加载订单详情
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('取消订单失败:', error)
      ElMessage.error(error.message || '取消失败')
    }
  }
}

// 完成订单
const handleComplete = async () => {
  if (!order.value) return

  try {
    await ElMessageBox.confirm('确定要完成该订单吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    await orderApi.completeOrder(order.value.id)
    ElMessage.success('订单已完成')
    loadOrderDetail() // 重新加载订单详情
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('完成订单失败:', error)
      ElMessage.error(error.message || '完成失败')
    }
  }
}

// 申请退款
const handleRefund = async () => {
  if (!order.value) return

  try {
    await ElMessageBox.confirm(
      `确定要申请退款吗？退款金额 ¥${order.value.payAmount || order.value.totalPrice} 将原路返回。`,
      '申请退款',
      {
        confirmButtonText: '确认退款',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    await orderApi.refundOrder(order.value.id)
    ElMessage.success('退款申请已提交，款项将在1-3个工作日内原路返回')
    loadOrderDetail() // 重新加载订单详情
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('申请退款失败:', error)
      ElMessage.error(error.message || '退款申请失败')
    }
  }
}

// 返回列表
const goBack = () => {
  router.push('/orders')
}

// 初始化
onMounted(() => {
  loadOrderDetail()
})
</script>

<style scoped>
.order-detail-container {
  padding: 20px;
  max-width: 1200px;
  margin: 0 auto;
}

.page-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 24px;
}

.page-header h1 {
  font-size: 28px;
  color: #303133;
  margin: 0;
}

.detail-sections {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.status-card {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.status-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.status-info {
  flex: 1;
}

.status-tag {
  margin-bottom: 16px;
  font-size: 16px;
  font-weight: 600;
}

.order-meta p {
  margin: 4px 0;
  font-size: 14px;
  opacity: 0.9;
}

.order-progress {
  flex: 1;
  max-width: 400px;
}

.items-card .card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.item-count {
  font-size: 14px;
  color: #909399;
}

.order-items {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.order-item {
  padding: 16px;
  background: #f5f7fa;
  border-radius: 4px;
  transition: background-color 0.3s;
}

.order-item:hover {
  background: #e4e7ed;
}

.product-image {
  width: 80px;
  height: 80px;
  border-radius: 4px;
  overflow: hidden;
}

.product-image .el-image {
  width: 100%;
  height: 100%;
}

.image-error {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
  background: #e4e7ed;
  color: #c0c4cc;
  font-size: 24px;
}

.product-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.product-name {
  font-size: 16px;
  color: #303133;
  margin: 0;
  font-weight: 500;
}

.product-sku {
  font-size: 12px;
  color: #909399;
  margin: 0;
}

.product-quantity {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
}

.product-price {
  display: flex;
  align-items: center;
  gap: 8px;
}

.product-price .label {
  font-size: 14px;
  color: #606266;
}

.product-price .price {
  font-size: 18px;
  font-weight: bold;
  color: #f56c6c;
}

.order-total {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid #e4e7ed;
}

.total-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
  font-size: 14px;
}

.total-item .label {
  color: #606266;
}

.total-item .value {
  color: #303133;
}

.total-item .total-price {
  font-size: 20px;
  font-weight: bold;
  color: #f56c6c;
}

.action-buttons {
  display: flex;
  justify-content: center;
  gap: 16px;
  padding: 20px 0;
}

.price-highlight {
  font-size: 18px;
  font-weight: bold;
  color: #f56c6c;
}

@media (max-width: 768px) {
  .order-detail-container {
    padding: 12px;
  }

  .status-content {
    flex-direction: column;
    gap: 16px;
  }

  .order-progress {
    max-width: 100%;
  }

  .action-buttons {
    flex-direction: column;
  }

  .action-buttons .el-button {
    width: 100%;
  }
}
</style>