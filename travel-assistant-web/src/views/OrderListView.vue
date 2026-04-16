<template>
  <div class="order-list-container">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-content">
        <div class="header-text">
          <h1>我的订单</h1>
          <p>查看和管理您的订单</p>
        </div>
        <el-button
          type="success"
          :icon="Download"
          @click="handleExport"
          :loading="exporting"
        >
          导出订单
        </el-button>
      </div>
    </div>

    <!-- 筛选器 -->
    <el-card class="filter-card">
      <el-row :gutter="16">
        <el-col :span="8">
          <el-input
            v-model="searchKeyword"
            placeholder="搜索订单号或商品名称"
            clearable
            @keyup.enter="handleSearch"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
        </el-col>

        <el-col :span="6">
          <el-select
            v-model="filterForm.status"
            placeholder="订单状态"
            clearable
            @change="handleFilter"
          >
            <el-option label="待支付" :value="0" />
            <el-option label="已支付" :value="1" />
            <el-option label="已完成" :value="2" />
            <el-option label="已取消" :value="3" />
            <el-option label="已退款" :value="4" />
          </el-select>
        </el-col>

        <el-col :span="10">
          <el-button type="primary" @click="handleSearch">
            <el-icon><Search /></el-icon>
            搜索
          </el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-col>
      </el-row>
    </el-card>

    <!-- 订单列表 -->
    <div v-loading="loading" class="order-list">
      <el-empty v-if="!loading && orders.length === 0" description="暂无订单数据" />

      <div v-else class="order-items">
        <el-card
          v-for="order in orders"
          :key="order.id"
          class="order-card"
          shadow="hover"
        >
          <!-- 订单头部 -->
          <div class="order-header">
            <div class="order-info">
              <span class="order-no">订单号：{{ order.orderNo }}</span>
              <span class="order-time">{{ formatDateTime(order.createTime) }}</span>
            </div>
            <el-tag :type="getOrderStatusColor(order.status)" size="large">
              {{ getOrderStatusText(order.status) }}
            </el-tag>
          </div>

          <el-divider />

          <!-- 订单内容 -->
          <div class="order-content">
            <!-- 订单明细列表 -->
            <div v-if="order.items && order.items.length > 0" class="order-items">
              <div
                v-for="item in order.items"
                :key="item.id"
                class="order-item"
              >
                <el-row :gutter="16">
                  <el-col :span="12">
                    <div class="product-info">
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
                      <div class="product-details">
                        <h3 class="product-name">{{ item.productName }}</h3>
                        <p class="product-quantity">数量：× {{ item.quantity }}</p>
                        <p class="product-price">单价：¥{{ item.price }}</p>
                      </div>
                    </div>
                  </el-col>

                  <el-col :span="12">
                    <div class="item-total">
                      <span class="label">小计：</span>
                      <span class="price">¥{{ item.totalPrice }}</span>
                    </div>
                  </el-col>
                </el-row>
              </div>
            </div>

            <!-- 兼容旧版本单商品订单 -->
            <div v-else class="order-items">
              <el-row :gutter="16">
                <el-col :span="12">
                  <div class="product-info">
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
                    <div class="product-details">
                      <h3 class="product-name">{{ order.productName }}</h3>
                      <p class="product-quantity">数量：× {{ order.quantity }}</p>
                    </div>
                  </div>
                </el-col>

                <el-col :span="12">
                  <div class="item-total">
                    <span class="label">小计：</span>
                    <span class="price">¥{{ order.totalPrice }}</span>
                  </div>
                </el-col>
              </el-row>
            </div>

            <!-- 订单总计和操作 -->
            <el-divider />

            <div class="order-footer">
              <div class="order-summary">
                <div v-if="order.payTime" class="detail-item">
                  <span class="label">支付时间：</span>
                  <span>{{ formatDateTime(order.payTime) }}</span>
                </div>

                <div v-if="order.remark" class="detail-item">
                  <span class="label">备注：</span>
                  <span>{{ order.remark }}</span>
                </div>

                <div class="detail-item total">
                  <span class="label">订单总价：</span>
                  <span class="price">¥{{ order.totalAmount || order.totalPrice }}</span>
                </div>
              </div>

              <div class="order-actions">
                <el-button
                  v-if="order.status === 0"
                  type="primary"
                  size="small"
                  @click="handlePay(order)"
                >
                  立即支付
                </el-button>
                <el-button
                  v-if="order.status === 0"
                  type="danger"
                  size="small"
                  @click="handleCancel(order)"
                >
                  取消订单
                </el-button>
                <el-button
                  v-if="order.status === 1"
                  type="success"
                  size="small"
                  @click="handleComplete(order)"
                >
                  完成订单
                </el-button>
                <el-button
                  v-if="order.status === 1"
                  type="warning"
                  size="small"
                  @click="handleRefund(order)"
                >
                  申请退款
                </el-button>
                <el-button
                  size="small"
                  @click="handleViewDetail(order)"
                >
                  查看详情
                </el-button>
              </div>
            </div>
          </div>
        </el-card>
      </div>
    </div>

    <!-- 分页 -->
    <div v-if="total > 0" class="pagination-container">
      <el-pagination
        v-model:current-page="pagination.page"
        v-model:page-size="pagination.size"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="handleSizeChange"
        @current-change="handlePageChange"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Download } from '@element-plus/icons-vue'
import { orderApi } from '@/api/order'
import { formatDateTime, getOrderStatusText, getOrderStatusColor } from '@/utils'
import type { Order } from '@/types'

const router = useRouter()

// 状态
const loading = ref(false)
const exporting = ref(false)
const orders = ref<Order[]>([])
const total = ref(0)
const searchKeyword = ref('')

// 筛选表单
const filterForm = reactive({
  status: undefined as number | undefined
})

// 分页
const pagination = reactive({
  page: 1,
  size: 10
})

// 加载订单列表
const loadOrders = async () => {
  loading.value = true
  try {
    const response = await orderApi.getUserOrders({
      page: pagination.page,
      size: pagination.size,
      ...filterForm
    })

    // 加载每个订单的明细
    const ordersWithItems = await Promise.all(
      response.records.map(async (order) => {
        try {
          const items = await orderApi.getOrderItems(order.id)
          return { ...order, items }
        } catch (error) {
          console.error(`加载订单 ${order.id} 明细失败:`, error)
          return order // 如果加载明细失败，返回原订单
        }
      })
    )

    orders.value = ordersWithItems
    total.value = response.total
  } catch (error) {
    console.error('加载订单列表失败:', error)
  } finally {
    loading.value = false
  }
}

// 筛选
const handleFilter = () => {
  pagination.page = 1
  loadOrders()
}

// 搜索
const handleSearch = async () => {
  if (!searchKeyword.value.trim()) {
    ElMessage.warning('请输入搜索关键词')
    return
  }

  loading.value = true
  pagination.page = 1
  try {
    const response = await orderApi.searchOrders({
      keyword: searchKeyword.value.trim(),
      page: pagination.page,
      size: pagination.size
    })

    // 加载订单明细
    const ordersWithItems = await Promise.all(
      response.records.map(async (order) => {
        try {
          const items = await orderApi.getOrderItems(order.id)
          return { ...order, items }
        } catch (error) {
          console.error(`加载订单 ${order.id} 明细失败:`, error)
          return order
        }
      })
    )

    orders.value = ordersWithItems
    total.value = response.total
  } catch (error) {
    console.error('搜索订单失败:', error)
  } finally {
    loading.value = false
  }
}

// 重置
const handleReset = () => {
  filterForm.status = undefined
  searchKeyword.value = ''
  pagination.page = 1
  loadOrders()
}

// 页码变化
const handlePageChange = (page: number) => {
  pagination.page = page
  loadOrders()
}

// 每页数量变化
const handleSizeChange = (size: number) => {
  pagination.size = size
  pagination.page = 1
  loadOrders()
}

// 支付订单
const handlePay = (order: Order) => {
  // 跳转到支付页面
  router.push(`/payment/${order.id}`)
}

// 取消订单
const handleCancel = async (order: Order) => {
  try {
    await ElMessageBox.confirm('确定要取消该订单吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    await orderApi.cancelOrder(order.id)
    ElMessage.success('订单已取消')
    loadOrders()
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('取消订单失败:', error)
    }
  }
}

// 完成订单
const handleComplete = async (order: Order) => {
  try {
    await ElMessageBox.confirm('确定要完成该订单吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    await orderApi.completeOrder(order.id)
    ElMessage.success('订单已完成')
    loadOrders()
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('完成订单失败:', error)
    }
  }
}

// 申请退款
const handleRefund = async (order: Order) => {
  try {
    await ElMessageBox.confirm(
      `确定要申请退款吗？退款金额 ¥${order.payAmount || order.totalPrice} 将原路返回。`,
      '申请退款',
      {
        confirmButtonText: '确认退款',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    await orderApi.refundOrder(order.id)
    ElMessage.success('退款申请已提交，款项将在1-3个工作日内原路返回')
    loadOrders()
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('申请退款失败:', error)
      ElMessage.error(error.message || '退款申请失败')
    }
  }
}

// 查看详情
const handleViewDetail = (order: Order) => {
  router.push(`/orders/${order.id}`)
}

// 导出订单数据
const handleExport = () => {
  try {
    exporting.value = true

    // 获取token
    const token = localStorage.getItem('token')
    if (!token) {
      ElMessage.error('请先登录')
      return
    }

    // 构建导出URL
    const exportUrl = orderApi.exportOrders()

    // 使用fetch进行下载
    fetch(exportUrl, {
      headers: {
        'Authorization': `Bearer ${token}`
      }
    })
    .then(response => {
      if (!response.ok) {
        throw new Error('导出失败')
      }
      return response.blob()
    })
    .then(blob => {
      const url = window.URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.href = url
      link.download = `订单列表_${new Date().getTime()}.xlsx`
      document.body.appendChild(link)
      link.click()
      document.body.removeChild(link)
      window.URL.revokeObjectURL(url)

      ElMessage.success('订单数据导出成功')
    })
    .catch(error => {
      console.error('导出订单数据失败:', error)
      ElMessage.error('导出订单数据失败')
    })
    .finally(() => {
      exporting.value = false
    })

  } catch (error) {
    console.error('导出订单数据失败:', error)
    ElMessage.error('导出订单数据失败')
    exporting.value = false
  }
}

// 初始化
onMounted(() => {
  loadOrders()
})
</script>

<style scoped>
.order-list-container {
  padding: 20px;
}

.page-header {
  margin-bottom: 20px;
}

.header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-text h1 {
  font-size: 28px;
  color: #303133;
  margin-bottom: 8px;
}

.header-text p {
  color: #909399;
  font-size: 14px;
  margin: 0;
}

.filter-card {
  margin-bottom: 20px;
}

.order-list {
  min-height: 400px;
}

.order-card {
  margin-bottom: 16px;
}

.order-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.order-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.order-no {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.order-time {
  font-size: 12px;
  color: #909399;
}

.order-content {
  padding: 16px 0;
}

.order-items {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.order-item {
  padding: 12px;
  background: #f5f7fa;
  border-radius: 4px;
}

.product-info {
  display: flex;
  gap: 16px;
}

.product-image {
  width: 80px;
  height: 80px;
  border-radius: 4px;
  overflow: hidden;
  flex-shrink: 0;
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

.product-details {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.product-name {
  font-size: 14px;
  color: #303133;
  margin: 0;
  font-weight: 500;
}

.product-quantity {
  font-size: 12px;
  color: #909399;
  margin: 0;
}

.product-price {
  font-size: 12px;
  color: #606266;
  margin: 0;
}

.item-total {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 8px;
}

.item-total .label {
  font-size: 14px;
  color: #606266;
}

.item-total .price {
  font-size: 16px;
  font-weight: bold;
  color: #f56c6c;
}

.order-footer {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  gap: 16px;
}

.order-summary {
  display: flex;
  flex-direction: column;
  gap: 8px;
  flex: 1;
}

.order-summary .detail-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.order-summary .detail-item .label {
  font-size: 14px;
  color: #606266;
}

.order-summary .detail-item.total {
  margin-top: 8px;
  padding-top: 8px;
  border-top: 1px solid #e4e7ed;
}

.order-summary .detail-item.total .label {
  font-size: 16px;
  font-weight: 500;
  color: #303133;
}

.order-summary .detail-item.total .price {
  font-size: 20px;
  font-weight: bold;
  color: #f56c6c;
}

.order-actions {
  display: flex;
  gap: 8px;
  flex-shrink: 0;
}

.pagination-container {
  display: flex;
  justify-content: center;
  margin-top: 24px;
}
</style>
