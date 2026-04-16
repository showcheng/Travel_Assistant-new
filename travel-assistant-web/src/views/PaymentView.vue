<template>
  <div class="payment-container">
    <!-- 页面头部 -->
    <div class="page-header">
      <el-button @click="goBack" :icon="ArrowLeft">返回</el-button>
      <h1>收银台</h1>
    </div>

    <div v-loading="loading" class="payment-content">
      <!-- 订单信息不存在 -->
      <el-empty v-if="!loading && !order" description="订单信息加载失败" />

      <!-- 支付流程 -->
      <div v-else-if="order" class="payment-flow">
        <!-- 订单信息确认 -->
        <el-card class="order-info-card">
          <template #header>
            <div class="card-header">
              <el-icon><ShoppingCart /></el-icon>
              <span>订单信息</span>
            </div>
          </template>

          <div class="order-info">
            <div class="order-meta">
              <h3 class="order-title">订单号：{{ order.orderNo }}</h3>
              <p class="order-time">创建时间：{{ formatDateTime(order.createTime) }}</p>
            </div>

            <el-divider />

            <!-- 商品清单 -->
            <div class="order-items">
              <div v-if="orderItems && orderItems.length > 0" class="items-list">
                <div
                  v-for="item in orderItems"
                  :key="item.id"
                  class="item-row"
                >
                  <span class="item-name">{{ item.productName }}</span>
                  <span class="item-quantity">× {{ item.quantity }}</span>
                  <span class="item-price">¥{{ item.totalPrice }}</span>
                </div>
              </div>

              <!-- 单商品兼容 -->
              <div v-else-if="order.productName" class="items-list">
                <div class="item-row">
                  <span class="item-name">{{ order.productName }}</span>
                  <span class="item-quantity">× {{ order.quantity }}</span>
                  <span class="item-price">¥{{ order.totalPrice }}</span>
                </div>
              </div>
            </div>

            <el-divider />

            <div class="order-total">
              <el-row>
                <el-col :span="12" />
                <el-col :span="12">
                  <div class="total-row">
                    <span class="label">订单总额：</span>
                    <span class="value">¥{{ order.totalAmount || order.totalPrice }}</span>
                  </div>
                  <div class="total-row main">
                    <span class="label">应付金额：</span>
                    <span class="value highlight">¥{{ order.payAmount || order.totalPrice }}</span>
                  </div>
                </el-col>
              </el-row>
            </div>
          </div>
        </el-card>

        <!-- 支付方式选择 -->
        <el-card class="payment-method-card">
          <template #header>
            <div class="card-header">
              <el-icon><Wallet /></el-icon>
              <span>选择支付方式</span>
            </div>
          </template>

          <div class="payment-methods">
            <div
              v-for="method in paymentMethods"
              :key="method.id"
              :class="['payment-method', { active: selectedMethod === method.id }]"
              @click="selectPaymentMethod(method.id)"
            >
              <div class="method-icon">
                <component :is="method.icon" />
              </div>
              <div class="method-info">
                <h4 class="method-name">{{ method.name }}</h4>
                <p class="method-desc">{{ method.description }}</p>
              </div>
              <div class="method-check">
                <el-radio
                  v-model="selectedMethod"
                  :label="method.id"
                  size="large"
                >
                  {{ '' }}
                </el-radio>
              </div>
            </div>
          </div>
        </el-card>

        <!-- 支付按钮 -->
        <div class="payment-actions">
          <el-button
            type="primary"
            size="large"
            :disabled="!selectedMethod"
            :loading="paying"
            @click="handlePayment"
          >
            <el-icon><Check /></el-icon>
            立即支付 ¥{{ order.payAmount || order.totalPrice }}
          </el-button>
          <el-button size="large" @click="goBack">
            取消支付
          </el-button>
        </div>

        <!-- 温馨提示 -->
        <el-card class="tips-card">
          <template #header>
            <div class="card-header">
              <el-icon><InfoFilled /></el-icon>
              <span>温馨提示</span>
            </div>
          </template>

          <ul class="tips-list">
            <li>请在30分钟内完成支付，超时订单将自动取消</li>
            <li>支付过程中请勿关闭浏览器</li>
            <li>如遇支付问题，请联系客服</li>
            <li>支付成功后，系统将自动跳转到订单详情页</li>
          </ul>
        </el-card>
      </div>
    </div>

    <!-- 支付结果对话框 -->
    <el-dialog
      v-model="resultDialogVisible"
      :title="resultDialogConfig.title"
      width="400px"
      :show-close="false"
      :close-on-click-modal="false"
      :close-on-press-escape="false"
    >
      <div class="result-content">
        <div :class="['result-icon', resultDialogConfig.type]">
          <component :is="resultDialogConfig.icon" />
        </div>
        <h3 class="result-title">{{ resultDialogConfig.title }}</h3>
        <p class="result-message">{{ resultDialogConfig.message }}</p>
      </div>

      <template #footer>
        <div class="result-actions">
          <el-button
            v-if="resultDialogConfig.type === 'success'"
            type="primary"
            @click="goToOrderDetail"
          >
            查看订单
          </el-button>
          <el-button
            v-else
            @click="goBack"
          >
            返回订单
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  ArrowLeft,
  ShoppingCart,
  Wallet,
  WechatPay,
  Alipay,
  CreditCard,
  Check,
  InfoFilled,
  SuccessFilled,
  CircleCloseFilled
} from '@element-plus/icons-vue'
import { orderApi } from '@/api/order'
import { formatDateTime } from '@/utils'
import type { Order, OrderItem } from '@/types'

const router = useRouter()
const route = useRoute()

// 状态
const loading = ref(false)
const paying = ref(false)
const order = ref<Order | null>(null)
const orderItems = ref<OrderItem[]>([])
const selectedMethod = ref<string>('')
const resultDialogVisible = ref(false)

// 支付方式配置
const paymentMethods = [
  {
    id: 'wechat',
    name: '微信支付',
    description: '推荐使用微信支付',
    icon: 'WechatPay'
  },
  {
    id: 'alipay',
    name: '支付宝',
    description: '安全快捷的支付方式',
    icon: 'Alipay'
  },
  {
    id: 'card',
    name: '银行卡支付',
    description: '支持储蓄卡和信用卡',
    icon: 'CreditCard'
  }
]

// 支付结果对话框配置
const resultDialogConfig = reactive({
  type: 'success',
  title: '',
  message: '',
  icon: SuccessFilled
})

// 加载订单信息
const loadOrderInfo = async () => {
  const orderId = Number(route.params.id || route.query.orderId)
  if (!orderId) {
    ElMessage.error('订单ID不存在')
    router.push('/orders')
    return
  }

  loading.value = true
  try {
    // 加载订单基本信息
    const orderData = await orderApi.getOrderById(orderId)
    order.value = orderData

    // 检查订单状态
    if (orderData.status !== 0) {
      ElMessage.warning('该订单不能支付')
      router.push(`/orders/${orderId}`)
      return
    }

    // 加载订单明细
    try {
      const items = await orderApi.getOrderItems(orderId)
      orderItems.value = items
    } catch (error) {
      console.error('加载订单明细失败:', error)
    }
  } catch (error: any) {
    console.error('加载订单信息失败:', error)
    ElMessage.error(error.message || '加载订单信息失败')
    goBack()
  } finally {
    loading.value = false
  }
}

// 选择支付方式
const selectPaymentMethod = (methodId: string) => {
  selectedMethod.value = methodId
}

// 处理支付
const handlePayment = async () => {
  if (!selectedMethod.value) {
    ElMessage.warning('请选择支付方式')
    return
  }

  if (!order.value) return

  try {
    await ElMessageBox.confirm(
      `确认使用${paymentMethods.find(m => m.id === selectedMethod.value)?.name}支付¥${order.value.payAmount || order.value.totalPrice}吗？`,
      '确认支付',
      {
        confirmButtonText: '确认支付',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    paying.value = true

    // 调用支付接口
    await orderApi.payOrder(order.value.id)

    // 支付成功
    showResultDialog('success', '支付成功', '您的订单已支付成功，感谢您的购买！')
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('支付失败:', error)
      showResultDialog('error', '支付失败', error.message || '支付过程中出现问题，请重试或联系客服')
    }
  } finally {
    paying.value = false
  }
}

// 显示结果对话框
const showResultDialog = (type: 'success' | 'error', title: string, message: string) => {
  resultDialogConfig.type = type
  resultDialogConfig.title = title
  resultDialogConfig.message = message
  resultDialogConfig.icon = type === 'success' ? SuccessFilled : CircleCloseFilled
  resultDialogVisible.value = true
}

// 查看订单详情
const goToOrderDetail = () => {
  if (order.value) {
    router.push(`/orders/${order.value.id}`)
  }
}

// 返回上一页
const goBack = () => {
  router.back()
}

// 初始化
onMounted(() => {
  loadOrderInfo()
})
</script>

<style scoped>
.payment-container {
  padding: 20px;
  max-width: 800px;
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

.payment-content {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.card-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.order-info-card, .payment-method-card, .tips-card {
  margin-bottom: 20px;
}

.order-info {
  padding: 16px 0;
}

.order-meta {
  margin-bottom: 16px;
}

.order-title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 8px 0;
}

.order-time {
  font-size: 14px;
  color: #909399;
  margin: 0;
}

.order-items {
  margin: 16px 0;
}

.items-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.item-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 0;
  font-size: 14px;
}

.item-name {
  flex: 1;
  color: #303133;
}

.item-quantity {
  color: #909399;
  margin: 0 16px;
}

.item-price {
  font-weight: 600;
  color: #f56c6c;
  min-width: 80px;
  text-align: right;
}

.order-total {
  margin-top: 16px;
}

.total-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
  font-size: 14px;
}

.total-row .label {
  color: #606266;
}

.total-row .value {
  color: #303133;
  font-weight: 500;
}

.total-row.main {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid #e4e7ed;
  font-size: 16px;
}

.total-row.main .label {
  font-size: 16px;
  font-weight: 500;
  color: #303133;
}

.total-row.main .value {
  font-size: 24px;
  font-weight: bold;
  color: #f56c6c;
}

.total-row.main .value.highlight {
  font-size: 28px;
}

.payment-methods {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.payment-method {
  display: flex;
  align-items: center;
  padding: 16px;
  border: 2px solid #e4e7ed;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s;
}

.payment-method:hover {
  border-color: #409eff;
  background: #f0f9ff;
}

.payment-method.active {
  border-color: #409eff;
  background: #ecf5ff;
}

.method-icon {
  width: 48px;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
  color: #409eff;
  margin-right: 16px;
}

.method-info {
  flex: 1;
}

.method-name {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 4px 0;
}

.method-desc {
  font-size: 14px;
  color: #909399;
  margin: 0;
}

.payment-actions {
  display: flex;
  justify-content: center;
  gap: 16px;
  padding: 20px 0;
}

.tips-card {
  background: #fef0f0;
  border-color: #fbc4c4;
}

.tips-list {
  margin: 0;
  padding-left: 20px;
}

.tips-list li {
  margin: 8px 0;
  color: #666;
  font-size: 14px;
}

.result-content {
  text-align: center;
  padding: 20px 0;
}

.result-icon {
  font-size: 64px;
  margin-bottom: 16px;
}

.result-icon.success {
  color: #67c23a;
}

.result-icon.error {
  color: #f56c6c;
}

.result-title {
  font-size: 20px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 8px 0;
}

.result-message {
  font-size: 14px;
  color: #606266;
  margin: 0;
}

.result-actions {
  display: flex;
  justify-content: center;
  gap: 12px;
}

@media (max-width: 768px) {
  .payment-container {
    padding: 12px;
  }

  .item-row {
    flex-direction: column;
    align-items: flex-start;
    gap: 4px;
  }

  .item-quantity {
    margin: 0;
  }

  .item-price {
    text-align: left;
  }

  .payment-actions {
    flex-direction: column;
  }

  .payment-actions .el-button {
    width: 100%;
  }
}
</style>