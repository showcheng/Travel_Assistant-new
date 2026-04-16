<template>
  <div v-loading="loading" class="product-detail-container">
    <el-empty v-if="!loading && !product" description="商品不存在或已下架" />

    <div v-else-if="product" class="product-detail">
      <!-- 返回按钮 -->
      <el-button class="back-button" @click="goBack">
        <el-icon><ArrowLeft /></el-icon>
        返回列表
      </el-button>

      <!-- 商品基本信息 -->
      <el-card class="product-info-card">
        <el-row :gutter="32">
          <el-col :span="12">
            <div class="product-image">
              <el-image
                :src="product.imageUrl || '/placeholder.jpg'"
                fit="cover"
                :preview-src-list="[product.imageUrl || '']"
              >
                <template #error>
                  <div class="image-error">
                    <el-icon :size="80"><Picture /></el-icon>
                  </div>
                </template>
              </el-image>
            </div>
          </el-col>

          <el-col :span="12">
            <div class="product-main">
              <h1 class="product-title">{{ product.name }}</h1>

              <el-tag v-if="product.status === 0" type="info" size="large">
                已下架
              </el-tag>
              <el-tag v-else type="success" size="large">
                上架中
              </el-tag>

              <div class="product-price-section">
                <div class="price-row">
                  <span class="price-label">现价：</span>
                  <span class="current-price">¥{{ product.currentPrice }}</span>
                </div>
                <div v-if="product.originalPrice > product.currentPrice" class="price-row">
                  <span class="price-label">原价：</span>
                  <span class="original-price">¥{{ product.originalPrice }}</span>
                  <span class="discount">
                    省¥{{ (product.originalPrice - product.currentPrice).toFixed(2) }}
                  </span>
                </div>
              </div>

              <el-divider />

              <div class="product-meta">
                <div class="meta-item">
                  <span class="label">商品类型：</span>
                  <el-tag size="small">{{ product.type }}</el-tag>
                </div>
                <div class="meta-item">
                  <span class="label">库存数量：</span>
                  <span :class="['stock', product.stock < 10 ? 'low' : '']">
                    {{ product.stock }}
                  </span>
                </div>
                <div class="meta-item">
                  <span class="label">创建时间：</span>
                  <span>{{ formatDateTime(product.createTime) }}</span>
                </div>
              </div>

              <el-divider />

              <div class="product-description">
                <h3>商品描述</h3>
                <p>{{ product.description }}</p>
              </div>

              <div class="action-buttons">
                <el-button
                  type="primary"
                  size="large"
                  :disabled="product.status === 0 || product.stock === 0"
                  @click="handleBuyNow"
                >
                  <el-icon><ShoppingCart /></el-icon>
                  立即购买
                </el-button>
                <el-button
                  size="large"
                  :disabled="product.status === 0 || product.stock === 0"
                  @click="handleAddToCart"
                >
                  <el-icon><Plus /></el-icon>
                  加入购物车
                </el-button>
              </div>
            </div>
          </el-col>
        </el-row>
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { productApi } from '@/api/product'
import { formatDateTime } from '@/utils'
import type { Product } from '@/types'

const router = useRouter()
const route = useRoute()

// 状态
const loading = ref(false)
const product = ref<Product | null>(null)

// 加载商品详情
const loadProductDetail = async () => {
  const productId = Number(route.params.id)
  if (!productId) {
    ElMessage.error('商品ID不存在')
    router.push('/products')
    return
  }

  loading.value = true
  try {
    product.value = await productApi.getProductById(productId)
  } catch (error) {
    console.error('加载商品详情失败:', error)
    ElMessage.error('商品不存在或已下架')
    router.push('/products')
  } finally {
    loading.value = false
  }
}

// 返回列表
const goBack = () => {
  router.push('/products')
}

// 立即购买
const handleBuyNow = () => {
  if (!product.value) return

  router.push({
    path: '/orders/create',
    query: {
      productId: product.value.id.toString(),
      quantity: '1'
    }
  })
}

// 加入购物车
const handleAddToCart = () => {
  // TODO: 实现购物车功能
  ElMessage.info('购物车功能开发中')
}

// 初始化
onMounted(() => {
  loadProductDetail()
})
</script>

<style scoped>
.product-detail-container {
  padding: 20px;
}

.product-detail {
  max-width: 1200px;
  margin: 0 auto;
}

.back-button {
  margin-bottom: 16px;
}

.product-info-card {
  margin-bottom: 20px;
}

.product-image {
  width: 100%;
  height: 400px;
  border-radius: 8px;
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
  background: #f5f7fa;
  color: #c0c4cc;
}

.product-main {
  padding: 20px 0;
}

.product-title {
  font-size: 32px;
  color: #303133;
  margin-bottom: 16px;
}

.product-price-section {
  margin: 24px 0;
}

.price-row {
  display: flex;
  align-items: baseline;
  margin-bottom: 8px;
}

.price-label {
  font-size: 16px;
  color: #606266;
  margin-right: 8px;
}

.current-price {
  font-size: 32px;
  font-weight: bold;
  color: #f56c6c;
}

.original-price {
  font-size: 18px;
  color: #909399;
  text-decoration: line-through;
}

.discount {
  margin-left: 12px;
  font-size: 14px;
  color: #f56c6c;
}

.product-meta {
  margin: 20px 0;
}

.meta-item {
  display: flex;
  align-items: center;
  margin-bottom: 12px;
}

.meta-item .label {
  font-size: 14px;
  color: #606266;
  margin-right: 8px;
}

.meta-item .stock {
  font-size: 14px;
  color: #67c23a;
}

.meta-item .stock.low {
  color: #e6a23c;
}

.product-description {
  margin: 20px 0;
}

.product-description h3 {
  font-size: 18px;
  color: #303133;
  margin-bottom: 12px;
}

.product-description p {
  font-size: 14px;
  color: #606266;
  line-height: 1.8;
}

.action-buttons {
  display: flex;
  gap: 12px;
  margin-top: 32px;
}

.action-buttons .el-button {
  flex: 1;
}
</style>
