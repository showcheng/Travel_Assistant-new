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
                :src="product.coverImage || '/placeholder.jpg'"
                fit="cover"
                lazy
              >
                <template #error>
                  <div class="image-error">
                    <el-icon :size="40"><Picture /></el-icon>
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
              <p class="product-description">{{ product.description }}</p>

              <div class="product-meta">
                <el-tag size="small" type="success">{{ product.type }}</el-tag>
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

// 状态
const loading = ref(false)
const products = ref<Product[]>([])
const total = ref(0)

// 搜索表单
const searchForm = reactive({
  keyword: '',
  type: '',
  status: 1 as number | undefined
})

// 分页
const pagination = reactive({
  page: 1,
  size: 12
})

// 加载商品列表
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

// 搜索
const handleSearch = () => {
  pagination.page = 1
  loadProducts()
}

// 重置
const handleReset = () => {
  searchForm.keyword = ''
  searchForm.type = ''
  searchForm.status = 1
  handleSearch()
}

// 页码变化
const handlePageChange = (page: number) => {
  pagination.page = page
  loadProducts()
}

// 每页数量变化
const handleSizeChange = (size: number) => {
  pagination.size = size
  pagination.page = 1
  loadProducts()
}

// 跳转详情页
const goToDetail = (id: number) => {
  router.push(`/products/${id}`)
}

// 初始化
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
  background: #f5f7fa;
  color: #c0c4cc;
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
