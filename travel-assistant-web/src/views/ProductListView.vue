<template>
  <div class="product-list-container">
    <!-- 页面头部 -->
    <div class="page-header">
      <h1>商品列表</h1>
      <p>浏览和选择您喜欢的旅游商品</p>
    </div>

    <!-- 搜索和筛选 -->
    <el-card class="filter-card">
      <el-row :gutter="16">
        <el-col :span="6">
          <el-input
            v-model="searchForm.keyword"
            placeholder="搜索商品名称"
            clearable
            @clear="handleSearch"
            @keyup.enter="handleSearch"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
        </el-col>

        <el-col :span="4">
          <el-select
            v-model="searchForm.type"
            placeholder="商品类型"
            clearable
            @change="handleSearch"
          >
            <el-option label="景点门票" value="ticket" />
            <el-option label="酒店住宿" value="hotel" />
            <el-option label="旅游套餐" value="package" />
            <el-option label="特色商品" value="merchandise" />
          </el-select>
        </el-col>

        <el-col :span="4">
          <el-select
            v-model="searchForm.status"
            placeholder="商品状态"
            clearable
            @change="handleSearch"
          >
            <el-option label="上架" :value="1" />
            <el-option label="下架" :value="0" />
          </el-select>
        </el-col>

        <el-col :span="6">
          <el-button type="primary" @click="handleSearch">
            <el-icon><Search /></el-icon>
            搜索
          </el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-col>
      </el-row>
    </el-card>

    <!-- 商品列表 -->
    <div v-loading="loading" class="product-list">
      <el-empty v-if="!loading && products.length === 0" description="暂无商品数据" />

      <el-row v-else :gutter="16">
        <el-col
          v-for="product in products"
          :key="product.id"
          :xs="24"
          :sm="12"
          :md="8"
          :lg="6"
          class="product-col"
        >
          <el-card class="product-card" shadow="hover" @click="goToDetail(product.id)">
            <div class="product-image">
              <el-image
                :src="getProductImage(product)"
                fit="cover"
                lazy
              >
                <template #error>
                  <div class="image-error">
                    <el-icon :size="40"><Picture /></el-icon>
                    <span>{{ getProductTypeName(product.type) }}</span>
                  </div>
                </template>
              </el-image>
              <el-tag
                v-if="product.status === 0"
                type="info"
                size="small"
                class="status-tag"
              >
                已下架
              </el-tag>
            </div>

            <div class="product-info">
              <h3 class="product-name">{{ product.name }}</h3>
              <p class="product-description">{{ product.description || '暂无描述' }}</p>

              <div class="product-meta">
                <el-tag size="small" :type="getTypeTagColor(product.type)">
                  {{ getProductTypeName(product.type) }}
                </el-tag>
                <el-tag size="small" type="info">
                  库存: {{ product.stock }}
                </el-tag>
              </div>

              <div class="product-price">
                <span class="current-price">¥{{ product.currentPrice }}</span>
                <span v-if="product.originalPrice > product.currentPrice" class="original-price">
                  ¥{{ product.originalPrice }}
                </span>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </div>

    <!-- 分页 -->
    <div v-if="total > 0" class="pagination-container">
      <el-pagination
        v-model:current-page="pagination.page"
        v-model:page-size="pagination.size"
        :total="total"
        :page-sizes="[10, 20, 50, 100]"
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
import { productApi } from '@/api/product'
import type { Product } from '@/types'

const router = useRouter()

const loading = ref(false)
const products = ref<Product[]>([])
const total = ref(0)

const searchForm = reactive({
  keyword: '',
  type: '',
  status: 1 as number | undefined
})

const pagination = reactive({
  page: 1,
  size: 12
})

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

const getProductImage = (product: Product): string => {
  if (product.coverImage) return product.coverImage
  if (product.imageUrl) return product.imageUrl

  const images = defaultImages[product.type] || defaultImages.ticket
  return images[(product.id || 0) % images.length]
}

const getProductTypeName = (type: string): string => {
  return typeNames[type] || type
}

const getTypeTagColor = (type: string): string => {
  return typeColors[type] || ''
}

const loadProducts = async () => {
  loading.value = true
  try {
    const response = await productApi.getProducts({
      page: pagination.page,
      size: pagination.size,
      ...searchForm
    })

    products.value = response.records
    total.value = response.total
  } catch (error) {
    console.error('加载商品列表失败:', error)
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pagination.page = 1
  loadProducts()
}

const handleReset = () => {
  searchForm.keyword = ''
  searchForm.type = ''
  searchForm.status = 1
  handleSearch()
}

const handlePageChange = (page: number) => {
  pagination.page = page
  loadProducts()
}

const handleSizeChange = (size: number) => {
  pagination.size = size
  pagination.page = 1
  loadProducts()
}

const goToDetail = (id: number) => {
  router.push(`/products/${id}`)
}

onMounted(() => {
  loadProducts()
})
</script>

<style scoped>
.product-list-container {
  padding: 20px;
}

.page-header {
  margin-bottom: 20px;
}

.page-header h1 {
  font-size: 28px;
  color: #303133;
  margin-bottom: 8px;
}

.page-header p {
  color: #909399;
  font-size: 14px;
  margin: 0;
}

.filter-card {
  margin-bottom: 20px;
}

.product-list {
  min-height: 400px;
}

.product-col {
  margin-bottom: 16px;
}

.product-card {
  cursor: pointer;
  transition: transform 0.3s, box-shadow 0.3s;
  height: 100%;
}

.product-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 16px rgba(0, 0, 0, 0.1);
}

.product-image {
  position: relative;
  width: 100%;
  height: 200px;
  margin-bottom: 12px;
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
  gap: 8px;
  font-size: 13px;
}

.status-tag {
  position: absolute;
  top: 8px;
  right: 8px;
}

.product-info {
  padding: 0 4px;
}

.product-name {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 8px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.product-description {
  font-size: 14px;
  color: #606266;
  margin-bottom: 12px;
  height: 40px;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.product-meta {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
}

.product-price {
  display: flex;
  align-items: baseline;
  gap: 8px;
}

.current-price {
  font-size: 20px;
  font-weight: bold;
  color: #f56c6c;
}

.original-price {
  font-size: 14px;
  color: #909399;
  text-decoration: line-through;
}

.pagination-container {
  display: flex;
  justify-content: center;
  margin-top: 24px;
}
</style>
