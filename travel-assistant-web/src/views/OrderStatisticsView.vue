<template>
  <div class="order-statistics-container">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-content">
        <div class="header-text">
          <h1>订单统计</h1>
          <p>查看您的订单数据统计</p>
        </div>
        <el-button
          type="primary"
          :icon="Download"
          @click="handleExport"
          :loading="exporting"
        >
          导出订单数据
        </el-button>
      </div>
    </div>

    <div v-loading="loading" class="statistics-content">
      <!-- 统计卡片 -->
      <el-row :gutter="20" class="statistics-cards">
        <el-col :xs="24" :sm="12" :md="6">
          <el-card class="stat-card">
            <div class="stat-content">
              <div class="stat-icon total">
                <el-icon><Document /></el-icon>
              </div>
              <div class="stat-info">
                <p class="stat-label">总订单数</p>
                <p class="stat-value">{{ statistics.totalOrders || 0 }}</p>
              </div>
            </div>
          </el-card>
        </el-col>

        <el-col :xs="24" :sm="12" :md="6">
          <el-card class="stat-card">
            <div class="stat-content">
              <div class="stat-icon amount">
                <el-icon><Money /></el-icon>
              </div>
              <div class="stat-info">
                <p class="stat-label">总消费金额</p>
                <p class="stat-value">¥{{ statistics.totalAmount?.toFixed(2) || '0.00' }}</p>
              </div>
            </div>
          </el-card>
        </el-col>

        <el-col :xs="24" :sm="12" :md="6">
          <el-card class="stat-card">
            <div class="stat-content">
              <div class="stat-icon today">
                <el-icon><Calendar /></el-icon>
              </div>
              <div class="stat-info">
                <p class="stat-label">今日订单</p>
                <p class="stat-value">{{ statistics.todayOrders || 0 }}</p>
              </div>
            </div>
          </el-card>
        </el-col>

        <el-col :xs="24" :sm="12" :md="6">
          <el-card class="stat-card">
            <div class="stat-content">
              <div class="stat-icon average">
                <el-icon><TrendCharts /></el-icon>
              </div>
              <div class="stat-info">
                <p class="stat-label">平均订单金额</p>
                <p class="stat-value">¥{{ statistics.averageAmount?.toFixed(2) || '0.00' }}</p>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <!-- 详细统计 -->
      <el-row :gutter="20" class="detail-statistics">
        <!-- 订单状态分布 -->
        <el-col :xs="24" :md="12">
          <el-card class="detail-card">
            <template #header>
              <div class="card-header">
                <el-icon><PieChart /></el-icon>
                <span>订单状态分布</span>
              </div>
            </template>

            <div class="status-distribution">
              <div
                v-for="(count, status) in statistics.statusCount"
                :key="status"
                class="status-item"
              >
                <div class="status-info">
                  <span class="status-name">{{ status }}</span>
                  <span class="status-count">{{ count }} 单</span>
                </div>
                <div class="status-bar">
                  <div
                    class="status-progress"
                    :style="{
                      width: getStatusPercentage(count) + '%',
                      backgroundColor: getStatusColor(status)
                    }"
                  />
                </div>
                <div class="status-amount">
                  ¥{{ formatAmount(statistics.statusAmount[status]) }}
                </div>
              </div>
            </div>
          </el-card>
        </el-col>

        <!-- 时间维度统计 -->
        <el-col :xs="24" :md="12">
          <el-card class="detail-card">
            <template #header>
              <div class="card-header">
                <el-icon><DataLine /></el-icon>
                <span>时间维度统计</span>
              </div>
            </template>

            <div class="time-statistics">
              <div class="time-item">
                <div class="time-header">
                  <span class="time-label">今日统计</span>
                  <el-tag type="success" size="small">今日</el-tag>
                </div>
                <div class="time-content">
                  <div class="time-row">
                    <span class="time-label">订单数量：</span>
                    <span class="time-value">{{ statistics.todayOrders || 0 }} 单</span>
                  </div>
                  <div class="time-row">
                    <span class="time-label">订单金额：</span>
                    <span class="time-value amount">¥{{ statistics.todayAmount?.toFixed(2) || '0.00' }}</span>
                  </div>
                </div>
              </div>

              <el-divider />

              <div class="time-item">
                <div class="time-header">
                  <span class="time-label">本月统计</span>
                  <el-tag type="primary" size="small">本月</el-tag>
                </div>
                <div class="time-content">
                  <div class="time-row">
                    <span class="time-label">订单数量：</span>
                    <span class="time-value">{{ statistics.monthOrders || 0 }} 单</span>
                  </div>
                  <div class="time-row">
                    <span class="time-label">订单金额：</span>
                    <span class="time-value amount">¥{{ statistics.monthAmount?.toFixed(2) || '0.00' }}</span>
                  </div>
                </div>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import {
  Document,
  Money,
  Calendar,
  TrendCharts,
  PieChart,
  DataLine,
  Download
} from '@element-plus/icons-vue'
import { orderApi, type OrderStatistics } from '@/api/order'

// 状态
const loading = ref(false)
const exporting = ref(false)
const statistics = ref<OrderStatistics>({
  totalOrders: 0,
  totalAmount: 0,
  statusCount: {},
  statusAmount: {},
  todayOrders: 0,
  todayAmount: 0,
  monthOrders: 0,
  monthAmount: 0,
  averageAmount: 0
})

// 加载统计数据
const loadStatistics = async () => {
  loading.value = true
  try {
    const data = await orderApi.getOrderStatistics()
    statistics.value = data
  } catch (error: any) {
    console.error('加载统计数据失败:', error)
    ElMessage.error(error.message || '加载统计数据失败')
  } finally {
    loading.value = false
  }
}

// 计算状态百分比
const getStatusPercentage = (count: number): number => {
  if (!statistics.value.totalOrders || statistics.value.totalOrders === 0) {
    return 0
  }
  return Math.round((count / statistics.value.totalOrders) * 100)
}

// 获取状态颜色
const getStatusColor = (status: string): string => {
  const colorMap: Record<string, string> = {
    '待支付': '#e6a23c',
    '已支付': '#67c23a',
    '已完成': '#909399',
    '已取消': '#f56c6c',
    '已退款': '#f56c6c'
  }
  return colorMap[status] || '#409eff'
}

// 格式化金额
const formatAmount = (amount: number | undefined): string => {
  if (!amount) return '0.00'
  return amount.toFixed(2)
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

    // 创建隐藏的a标签进行下载
    const link = document.createElement('a')
    link.href = exportUrl
    link.setAttribute('download', `orders_${new Date().getTime()}.xlsx`)

    // 设置Authorization头
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
      link.href = url
      link.download = `订单数据_${new Date().getTime()}.xlsx`
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
  loadStatistics()
})
</script>

<style scoped>
.order-statistics-container {
  padding: 20px;
  max-width: 1400px;
  margin: 0 auto;
}

.page-header {
  margin-bottom: 24px;
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

.statistics-content {
  min-height: 400px;
}

/* 统计卡片 */
.statistics-cards {
  margin-bottom: 24px;
}

.stat-card {
  margin-bottom: 20px;
  transition: transform 0.3s, box-shadow 0.3s;
}

.stat-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.stat-content {
  display: flex;
  align-items: center;
  gap: 16px;
}

.stat-icon {
  width: 60px;
  height: 60px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
  color: white;
}

.stat-icon.total {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.stat-icon.amount {
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
}

.stat-icon.today {
  background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
}

.stat-icon.average {
  background: linear-gradient(135deg, #43e97b 0%, #38f9d7 100%);
}

.stat-info {
  flex: 1;
}

.stat-label {
  font-size: 14px;
  color: #909399;
  margin: 0 0 4px 0;
}

.stat-value {
  font-size: 24px;
  font-weight: bold;
  color: #303133;
  margin: 0;
}

/* 详细统计 */
.detail-statistics {
  margin-top: 24px;
}

.detail-card {
  margin-bottom: 20px;
  height: 100%;
}

.card-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

/* 状态分布 */
.status-distribution {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.status-item {
  padding: 12px;
  background: #f5f7fa;
  border-radius: 6px;
}

.status-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.status-name {
  font-size: 14px;
  font-weight: 500;
  color: #303133;
}

.status-count {
  font-size: 14px;
  color: #606266;
}

.status-bar {
  height: 8px;
  background: #e4e7ed;
  border-radius: 4px;
  overflow: hidden;
  margin-bottom: 8px;
}

.status-progress {
  height: 100%;
  transition: width 0.3s ease;
}

.status-amount {
  text-align: right;
  font-size: 12px;
  color: #909399;
}

/* 时间统计 */
.time-statistics {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.time-item {
  padding: 16px;
  background: #f5f7fa;
  border-radius: 6px;
}

.time-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.time-item .time-label {
  font-size: 14px;
  font-weight: 500;
  color: #303133;
}

.time-content {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.time-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.time-row .time-label {
  font-size: 13px;
  color: #606266;
}

.time-value {
  font-size: 14px;
  font-weight: 500;
  color: #303133;
}

.time-value.amount {
  color: #f56c6c;
  font-weight: 600;
}

@media (max-width: 768px) {
  .order-statistics-container {
    padding: 12px;
  }

  .statistics-cards .el-col {
    margin-bottom: 12px;
  }

  .stat-value {
    font-size: 20px;
  }

  .detail-statistics .el-col {
    margin-bottom: 16px;
  }
}
</style>