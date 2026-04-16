<template>
  <div v-loading="loading" class="create-order-container">
    <el-page-header @back="goBack" title="返回商品列表">
      <template #content>
        <span class="page-title">创建订单</span>
      </template>
    </el-page-header>

    <div class="order-content">
      <!-- 购物车 -->
      <el-card class="cart-card">
        <template #header>
          <div class="card-header">
            <span>商品清单</span>
            <el-button type="text" @click="showAddProductDialog">
              <el-icon><Plus /></el-icon>
              添加商品
            </el-button>
          </div>
        </template>

        <el-empty v-if="cartItems.length === 0" description="购物车为空，请添加商品">
          <el-button type="primary" @click="showAddProductDialog">添加商品</el-button>
        </el-empty>

        <div v-else class="cart-items">
          <div
            v-for="(item, index) in cartItems"
            :key="index"
            class="cart-item"
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

              <el-col :span="8">
                <div class="product-info">
                  <h4 class="product-name">{{ item.productName }}</h4>
                  <p class="product-id">ID: {{ item.productId }}</p>
                </div>
              </el-col>

              <el-col :span="6">
                <div class="quantity-control">
                  <el-input-number
                    v-model="item.quantity"
                    :min="1"
                    :max="item.maxStock"
                    :precision="0"
                    size="small"
                    @change="updateItemTotal(index)"
                  />
                  <span class="stock-info">库存: {{ item.maxStock }}</span>
                </div>
              </el-col>

              <el-col :span="5">
                <div class="item-total">
                  <span class="price">¥{{ item.totalPrice }}</span>
                  <el-button
                    type="danger"
                    size="small"
                    text
                    @click="removeCartItem(index)"
                  >
                    <el-icon><Delete /></el-icon>
                    移除
                  </el-button>
                </div>
              </el-col>
            </el-row>
          </div>

          <!-- 订单总计 -->
          <div class="cart-total">
            <el-row>
              <el-col :span="12" />
              <el-col :span="12">
                <div class="total-item">
                  <span class="label">商品总额：</span>
                  <span class="value">¥{{ cartTotal }}</span>
                </div>
              </el-col>
            </el-row>
          </div>
        </div>
      </el-card>

      <!-- 订单表单 -->
      <el-card class="order-form-card">
        <template #header>
          <div class="card-header">
            <span>订单信息</span>
          </div>
        </template>

        <el-form
          ref="orderFormRef"
          :model="orderForm"
          :rules="orderRules"
          label-width="100px"
        >
          <el-form-item label="订单备注" prop="remark">
            <el-input
              v-model="orderForm.remark"
              type="textarea"
              :rows="3"
              placeholder="请输入订单备注（可选）"
              maxlength="500"
              show-word-limit
            />
          </el-form-item>

          <el-form-item>
            <el-button
              type="primary"
              size="large"
              :loading="submitting"
              :disabled="cartItems.length === 0"
              @click="handleSubmit"
            >
              <el-icon><Check /></el-icon>
              提交订单
            </el-button>
            <el-button size="large" @click="goBack">
              取消
            </el-button>
          </el-form-item>
        </el-form>
      </el-card>
    </div>

    <!-- 添加商品对话框 -->
    <el-dialog
      v-model="addProductDialogVisible"
      title="添加商品"
      width="600px"
    >
      <el-input
        v-model="searchKeyword"
        placeholder="搜索商品名称"
        clearable
        @input="handleSearch"
      >
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
      </el-input>

      <div v-loading="searching" class="product-list">
        <el-empty v-if="!searching && searchResults.length === 0" description="未找到相关商品" />

        <div v-else class="search-results">
          <div
            v-for="product in searchResults"
            :key="product.id"
            class="search-result-item"
            @click="addProductToCart(product)"
          >
            <el-row :gutter="12" align="middle">
              <el-col :span="6">
                <div class="product-image">
                  <el-image
                    :src="product.imageUrl || '/placeholder.jpg'"
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

              <el-col :span="12">
                <div class="product-info">
                  <h4 class="product-name">{{ product.name }}</h4>
                  <p class="product-price">¥{{ product.currentPrice }}</p>
                </div>
              </el-col>

              <el-col :span="6">
                <el-button type="primary" size="small" @click="addProductToCart(product)">
                  <el-icon><Plus /></el-icon>
                  添加
                </el-button>
              </el-col>
            </el-row>
          </div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, FormInstance, FormRules } from 'element-plus'
import { Plus, Picture, Delete, Search, Check } from '@element-plus/icons-vue'
import { productApi } from '@/api/product'
import { orderApi } from '@/api/order'
import type { Product } from '@/types'
import type { OrderItemCreateRequest } from '@/types'

const router = useRouter()
const route = useRoute()
const orderFormRef = ref<FormInstance>()

// 购物车项目类型
interface CartItem extends OrderItemCreateRequest {
  productName: string
  productImage?: string
  maxStock: number
  totalPrice: number
}

// 状态
const loading = ref(false)
const submitting = ref(false)
const addProductDialogVisible = ref(false)
const searching = ref(false)
const searchKeyword = ref('')
const searchResults = ref<Product[]>([])

// 购物车数据
const cartItems = ref<CartItem[]>([])

// 订单表单
const orderForm = reactive({
  remark: ''
})

// 表单验证规则
const orderRules: FormRules = {
  remark: [
    { max: 500, message: '备注长度不能超过500个字符', trigger: 'blur' }
  ]
}

// 计算购物车总金额
const cartTotal = computed(() => {
  return cartItems.value
    .reduce((sum, item) => sum + item.totalPrice, 0)
    .toFixed(2)
})

// 显示添加商品对话框
const showAddProductDialog = async () => {
  addProductDialogVisible.value = true
  if (searchResults.value.length === 0) {
    await loadAllProducts()
  }
}

// 加载所有商品
const loadAllProducts = async () => {
  searching.value = true
  try {
    const result = await productApi.getProducts({
      page: 1,
      size: 50,
      status: '1' // 只显示上架商品
    })
    searchResults.value = result.records || []
  } catch (error) {
    console.error('加载商品列表失败:', error)
    ElMessage.error('加载商品列表失败')
  } finally {
    searching.value = false
  }
}

// 搜索商品
const handleSearch = async () => {
  if (!searchKeyword.value.trim()) {
    await loadAllProducts()
    return
  }

  searching.value = true
  try {
    const result = await productApi.getProducts({
      page: 1,
      size: 20,
      status: '1'
    })
    // 前端过滤搜索结果
    searchResults.value = (result.records || []).filter(
      product => product.name.toLowerCase().includes(searchKeyword.value.toLowerCase())
    )
  } catch (error) {
    console.error('搜索商品失败:', error)
    ElMessage.error('搜索商品失败')
  } finally {
    searching.value = false
  }
}

// 添加商品到购物车
const addProductToCart = (product: Product) => {
  // 检查商品是否已在购物车中
  const existingItem = cartItems.value.find(item => item.productId === product.id)

  if (existingItem) {
    // 如果已存在，增加数量
    if (existingItem.quantity < product.stock) {
      existingItem.quantity++
      existingItem.totalPrice = existingItem.quantity * existingItem.price
      ElMessage.success('商品数量已增加')
    } else {
      ElMessage.warning('已达到最大库存数量')
    }
  } else {
    // 添加新商品到购物车
    cartItems.value.push({
      productId: product.id,
      productName: product.name,
      productImage: product.imageUrl,
      quantity: 1,
      price: product.currentPrice,
      maxStock: product.stock,
      totalPrice: product.currentPrice
    })
    ElMessage.success('商品已添加到购物车')
  }

  addProductDialogVisible.value = false
  searchKeyword.value = ''
}

// 移除购物车项目
const removeCartItem = (index: number) => {
  cartItems.value.splice(index, 1)
  ElMessage.success('商品已移除')
}

// 更新项目总价
const updateItemTotal = (index: number) => {
  const item = cartItems.value[index]
  if (item.quantity > item.maxStock) {
    item.quantity = item.maxStock
    ElMessage.warning('已达到最大库存数量')
  }
  item.totalPrice = item.quantity * item.price
}

// 返回上一页
const goBack = () => {
  router.back()
}

// 提交订单
const handleSubmit = async () => {
  if (!orderFormRef.value) return

  if (cartItems.value.length === 0) {
    ElMessage.warning('请先添加商品到购物车')
    return
  }

  try {
    await orderFormRef.value.validate()

    submitting.value = true

    // 构建订单明细
    const items = cartItems.value.map(item => ({
      productId: item.productId,
      productName: item.productName,
      productImage: item.productImage,
      quantity: item.quantity,
      price: item.price
    }))

    const orderId = await orderApi.createOrder({
      items,
      remark: orderForm.remark
    })

    ElMessage.success('订单创建成功')

    // 跳转到订单详情页
    router.push(`/orders/${orderId}`)
  } catch (error: any) {
    console.error('创建订单失败:', error)
    ElMessage.error(error.message || '创建订单失败')
  } finally {
    submitting.value = false
  }
}

// 初始化时检查是否有预选商品
onMounted(async () => {
  const productId = route.query.productId
  if (productId) {
    try {
      loading.value = true
      const product = await productApi.getProductById(Number(productId))

      cartItems.value.push({
        productId: product.id,
        productName: product.name,
        productImage: product.imageUrl,
        quantity: Number(route.query.quantity) || 1,
        price: product.currentPrice,
        maxStock: product.stock,
        totalPrice: product.currentPrice * (Number(route.query.quantity) || 1)
      })
    } catch (error) {
      console.error('加载商品信息失败:', error)
      ElMessage.error('加载商品信息失败')
    } finally {
      loading.value = false
    }
  }
})
</script>

<style scoped>
.create-order-container {
  padding: 20px;
  max-width: 1000px;
  margin: 0 auto;
}

.page-title {
  font-size: 20px;
  font-weight: 600;
  color: #303133;
}

.order-content {
  margin-top: 20px;
}

.cart-card, .order-form-card {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.cart-items {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.cart-item {
  padding: 16px;
  background: #f5f7fa;
  border-radius: 4px;
  transition: background-color 0.3s;
}

.cart-item:hover {
  background: #e4e7ed;
}

.product-image {
  width: 60px;
  height: 60px;
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
  font-size: 20px;
}

.product-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.product-name {
  font-size: 14px;
  font-weight: 500;
  color: #303133;
  margin: 0;
}

.product-id {
  font-size: 12px;
  color: #909399;
  margin: 0;
}

.product-price {
  font-size: 16px;
  font-weight: bold;
  color: #f56c6c;
  margin: 0;
}

.quantity-control {
  display: flex;
  flex-direction: column;
  gap: 4px;
  align-items: center;
}

.stock-info {
  font-size: 12px;
  color: #909399;
}

.item-total {
  display: flex;
  flex-direction: column;
  gap: 8px;
  align-items: flex-end;
}

.item-total .price {
  font-size: 16px;
  font-weight: bold;
  color: #f56c6c;
}

.cart-total {
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
  font-size: 18px;
  font-weight: bold;
  color: #f56c6c;
}

.product-list {
  margin-top: 16px;
  min-height: 200px;
}

.search-results {
  display: flex;
  flex-direction: column;
  gap: 8px;
  max-height: 400px;
  overflow-y: auto;
}

.search-result-item {
  padding: 12px;
  background: #f5f7fa;
  border-radius: 4px;
  cursor: pointer;
  transition: background-color 0.3s;
}

.search-result-item:hover {
  background: #e4e7ed;
}

@media (max-width: 768px) {
  .create-order-container {
    padding: 12px;
  }

  .cart-item .el-col {
    margin-bottom: 8px;
  }

  .item-total {
    align-items: flex-start;
  }
}
</style>
