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
                :src="getProductImage(product)"
                fit="cover"
                :preview-src-list="[getProductImage(product)]"
              >
                <template #error>
                  <div class="image-error">
                    <el-icon :size="80"><Picture /></el-icon>
                    <span>{{ getTypeName(product.type) }}</span>
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
                  <el-tag size="small" :type="getTypeTagColor(product.type)">
                    {{ getTypeName(product.type) }}
                  </el-tag>
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
                <p>{{ product.description || '暂无详细描述' }}</p>
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

const loading = ref(false)
const product = ref<Product | null>(null)

const typeNames: Record<string, string> = {
  ticket: '景点门票',
  hotel: '酒店住宿',
  package: '旅游套餐',
  merchandise: '特色商品'
}

const typeColors: Record<string, string> = {
  ticket: 'success',
  hotel: 'warning',
  package: 'danger',
  merchandise: 'info'
}

const defaultImages: Record<string, string[]> = {
  ticket: [
    'https://images.unsplash.com/photo-1467269204594-9661b134dd2b?w=800&h=600&fit=crop',
    'https://images.unsplash.com/photo-1506929562872-bb421503ef21?w=800&h=600&fit=crop',
    'https://images.unsplash.com/photo-1476514525535-07fb3b4ae5f1?w=800&h=600&fit=crop',
    'https://images.unsplash.com/photo-1501785888041-af3ef285b470?w=800&h=600&fit=crop'
  ],
  hotel: [
    'https://images.unsplash.com/photo-1566073771259-6a8506099945?w=800&h=600&fit=crop',
    'https://images.unsplash.com/photo-1582719508461-905c673771fd?w=800&h=600&fit=crop',
    'https://images.unsplash.com/photo-1551882547-ff40c63fe5fa?w=800&h=600&fit=crop',
    'https://images.unsplash.com/photo-1445019980597-93fa8acb246c?w=800&h=600&fit=crop'
  ],
  package: [
    'https://images.unsplash.com/photo-1488646953014-85cb44e25828?w=800&h=600&fit=crop',
    'https://images.unsplash.com/photo-1503220317375-aaad61436b1b?w=800&h=600&fit=crop',
    'https://images.unsplash.com/photo-1530789253388-582c481c54b0?w=800&h=600&fit=crop',
    'https://images.unsplash.com/photo-1469854523086-cc02fe5d8800?w=800&h=600&fit=crop'
  ],
  merchandise: [
    'https://images.unsplash.com/photo-1556742049-0cfed4f6a45d?w=800&h=600&fit=crop',
    'https://images.unsplash.com/photo-1607344645866-009c320b63e0?w=800&h=600&fit=crop',
    'https://images.unsplash.com/photo-1607082349566-187342175e2f?w=800&h=600&fit=crop',
    'https://images.unsplash.com/photo-1523275335684-37898b6baf30?w=800&h=600&fit=crop'
  ]
}

const getProductImage = (p: Product): string => {
  if (p.coverImage) return p.coverImage
  if (p.imageUrl) return p.imageUrl
  const images = defaultImages[p.type] || defaultImages.ticket
  return images[(p.id || 0) % images.length]
}

const getTypeName = (type: string): string => {
  return typeNames[type] || type
}

const getTypeTagColor = (type: string): string => {
  return typeColors[type] || ''
}

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

const goBack = () => {
  router.push('/products')
}

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

const handleAddToCart = () => {
  ElMessage.info('购物车功能开发中')
}

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
  flex-direction: column;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
  background: linear-gradient(135deg, #f5f7fa 0%, #e4e7ed 100%);
  color: #c0c4cc;
  gap: 12px;
  font-size: 16px;
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
