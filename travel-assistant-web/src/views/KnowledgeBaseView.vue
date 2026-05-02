<template>
  <el-container class="knowledge-base-container">
    <!-- 页面标题 -->
    <el-header class="page-header">
      <div class="header-content">
        <div class="title-section">
          <el-icon :size="28" color="#409eff"><Reading /></el-icon>
          <span class="page-title">知识库管理</span>
        </div>
        <div class="action-section">
          <el-button type="primary" :icon="Plus" @click="showUploadDialog">
            上传文档
          </el-button>
          <el-button :icon="Refresh" @click="loadDocuments">刷新</el-button>
        </div>
      </div>
    </el-header>

    <!-- 主要内容区域 -->
    <el-main class="main-content">
      <!-- 统计信息卡片 -->
      <el-row :gutter="20" class="stats-row">
        <el-col :span="6">
          <el-card class="stat-card">
            <div class="stat-content">
              <el-icon :size="40" color="#409eff"><Document /></el-icon>
              <div class="stat-info">
                <div class="stat-value">{{ stats.totalDocuments }}</div>
                <div class="stat-label">总文档数</div>
              </div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card class="stat-card">
            <div class="stat-content">
              <el-icon :size="40" color="#67C23A"><Collection /></el-icon>
              <div class="stat-info">
                <div class="stat-value">{{ stats.totalChunks }}</div>
                <div class="stat-label">总分块数</div>
              </div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card class="stat-card">
            <div class="stat-content">
              <el-icon :size="40" color="#E6A23C"><Grid /></el-icon>
              <div class="stat-info">
                <div class="stat-value">{{ stats.totalVectors }}</div>
                <div class="stat-label">向量数量</div>
              </div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card class="stat-card">
            <div class="stat-content">
              <el-icon :size="40" color="#F56C6C"><DataAnalysis /></el-icon>
              <div class="stat-info">
                <div class="stat-value">{{ stats.categories }}</div>
                <div class="stat-label">分类数</div>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <!-- 图表区域 -->
      <div class="chart-row">
        <el-card class="chart-card">
          <template #header>
            <span>类别分布</span>
          </template>
          <div ref="pieChartRef" style="height: 300px"></div>
        </el-card>
        <el-card class="chart-card">
          <template #header>
            <span>检索趋势 (近7天)</span>
          </template>
          <div ref="lineChartRef" style="height: 300px"></div>
        </el-card>
      </div>

      <!-- 筛选和搜索区域 -->
      <el-card class="search-card">
        <el-row :gutter="20">
          <el-col :span="8">
            <el-select
              v-model="filters.category"
              placeholder="选择分类"
              clearable
              @change="handleFilterChange"
              style="width: 100%"
            >
              <el-option label="全部分类" value="" />
              <el-option label="景点介绍" value="ATTRACTION" />
              <el-option label="政策说明" value="POLICY" />
              <el-option label="路线推荐" value="ROUTE" />
              <el-option label="常见问题" value="FAQ" />
            </el-select>
          </el-col>
          <el-col :span="8">
            <el-input
              v-model="filters.keyword"
              placeholder="搜索文档标题"
              :prefix-icon="Search"
              clearable
              @input="handleSearch"
            />
          </el-col>
          <el-col :span="8">
            <el-radio-group v-model="filters.status" @change="handleFilterChange">
              <el-radio-button label="">全部</el-radio-button>
              <el-radio-button label="COMPLETED">已完成</el-radio-button>
              <el-radio-button label="PROCESSING">处理中</el-radio-button>
            </el-radio-group>
          </el-col>
        </el-row>
      </el-card>

      <!-- 文档列表 -->
      <el-card class="documents-card">
        <el-table :data="paginatedDocuments" style="width: 100%" v-loading="loading">
          <el-table-column prop="title" label="文档标题" min-width="240">
            <template #default="scope">
              <div class="title-cell">
                <el-icon :size="18" :color="getCategoryColor(scope.row.category)">
                  <component :is="getCategoryIcon(scope.row.category)" />
                </el-icon>
                <el-text class="document-title" truncated>{{ scope.row.title }}</el-text>
                <el-tag
                  :type="getCategoryTagType(scope.row.category)"
                  size="small"
                >
                  {{ getCategoryName(scope.row.category) }}
                </el-tag>
              </div>
            </template>
          </el-table-column>

          <el-table-column prop="status" label="状态" width="100" align="center">
            <template #default="scope">
              <el-tag :type="getStatusTagType(scope.row.status)" size="small">
                {{ getStatusName(scope.row.status) }}
              </el-tag>
            </template>
          </el-table-column>

          <el-table-column prop="chunkCount" label="分块数" width="100" align="center">
            <template #default="scope">
              <el-tag type="info" size="small">{{ scope.row.chunkCount || 0 }}</el-tag>
            </template>
          </el-table-column>

          <el-table-column prop="uploadTime" label="上传时间" width="180">
            <template #default="scope">
              {{ formatDateTime(scope.row.uploadTime) }}
            </template>
          </el-table-column>

          <el-table-column label="操作" width="200" fixed="right" align="center">
            <template #default="scope">
              <el-button
                type="primary"
                size="small"
                :icon="View"
                @click="viewDocument(scope.row)"
                link
              >
                查看
              </el-button>
              <el-button
                type="danger"
                size="small"
                :icon="Delete"
                @click="deleteDocument(scope.row)"
                link
              >
                删除
              </el-button>
            </template>
          </el-table-column>
        </el-table>

        <!-- 分页 -->
        <div class="pagination-container">
          <el-pagination
            v-model:current-page="pagination.currentPage"
            v-model:page-size="pagination.pageSize"
            :page-sizes="[10, 20, 50, 100]"
            :total="filteredDocuments.length"
            layout="total, sizes, prev, pager, next, jumper"
            @size-change="handleSizeChange"
            @current-change="handleCurrentChange"
          />
        </div>
      </el-card>
    </el-main>

    <!-- 上传文档对话框 -->
    <el-dialog
      v-model="uploadDialog.visible"
      title="上传知识库文档"
      width="650px"
      :close-on-click-modal="false"
    >
      <!-- 上传模式切换 -->
      <el-tabs v-model="uploadMode" class="upload-tabs">
        <!-- 文件上传模式 -->
        <el-tab-pane label="文件上传" name="file">
          <el-form :model="uploadForm" ref="uploadFormRef" label-width="100px">
            <el-form-item label="选择文件">
              <el-upload
                ref="uploadRef"
                class="upload-area"
                drag
                :auto-upload="false"
                :limit="1"
                accept=".pdf,.txt"
                :on-exceed="handleExceed"
                :before-upload="beforeFileUpload"
              >
                <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
                <div class="el-upload__text">拖拽文件到此处或<em>点击上传</em></div>
                <template #tip>
                  <div class="el-upload__tip">支持 PDF、TXT 格式，文件大小不超过10MB</div>
                </template>
              </el-upload>
            </el-form-item>

            <el-form-item label="文档标题">
              <el-input
                v-model="uploadForm.title"
                placeholder="不填则使用文件名"
                maxlength="200"
                show-word-limit
              />
            </el-form-item>

            <el-form-item label="文档分类">
              <el-select v-model="uploadForm.category" placeholder="请选择文档分类" style="width: 100%">
                <el-option label="景点介绍" value="ATTRACTION" />
                <el-option label="政策说明" value="POLICY" />
                <el-option label="路线推荐" value="ROUTE" />
                <el-option label="常见问题" value="FAQ" />
                <el-option label="未分类" value="未分类" />
              </el-select>
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <!-- 文本输入模式（保留原有功能） -->
        <el-tab-pane label="文本输入" name="text">
          <el-form :model="uploadForm" :rules="uploadRules" ref="textUploadFormRef" label-width="100px">
            <el-form-item label="文档标题" prop="title">
              <el-input
                v-model="uploadForm.title"
                placeholder="请输入文档标题"
                maxlength="200"
                show-word-limit
              />
            </el-form-item>

            <el-form-item label="文档分类" prop="category">
              <el-select v-model="uploadForm.category" placeholder="请选择文档分类" style="width: 100%">
                <el-option label="景点介绍" value="ATTRACTION" />
                <el-option label="政策说明" value="POLICY" />
                <el-option label="路线推荐" value="ROUTE" />
                <el-option label="常见问题" value="FAQ" />
              </el-select>
            </el-form-item>

            <el-form-item label="文档内容" prop="content">
              <el-input
                v-model="uploadForm.content"
                type="textarea"
                :rows="8"
                placeholder="请输入文档内容，支持纯文本格式"
                maxlength="10000"
                show-word-limit
              />
            </el-form-item>

            <el-form-item label="文件类型">
              <el-input v-model="uploadForm.fileType" placeholder="自动识别或手动输入" />
            </el-form-item>
          </el-form>
        </el-tab-pane>
      </el-tabs>

      <template #footer>
        <el-button @click="uploadDialog.visible = false">取消</el-button>
        <el-button type="primary" @click="submitUpload" :loading="uploadDialog.uploading">
          上传
        </el-button>
      </template>
    </el-dialog>

    <!-- 查看文档对话框 -->
    <el-dialog
      v-model="viewDialog.visible"
      :title="viewDialog.title"
      width="800px"
    >
      <el-descriptions :column="2" border>
        <el-descriptions-item label="文档ID">{{ viewDialog.docId }}</el-descriptions-item>
        <el-descriptions-item label="分类">
          <el-tag :type="getCategoryTagType(viewDialog.category)">
            {{ getCategoryName(viewDialog.category) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="getStatusTagType(viewDialog.status)">
            {{ getStatusName(viewDialog.status) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="分块数">
          <el-tag type="info">{{ viewDialog.chunkCount }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="上传时间">
          {{ formatDateTime(viewDialog.uploadTime) }}
        </el-descriptions-item>
      </el-descriptions>

      <el-divider content-position="left">文档内容</el-divider>

      <div class="document-content">
        <p class="content-text">{{ viewDialog.content }}</p>
      </div>

      <template #footer>
        <el-button @click="viewDialog.visible = false">关闭</el-button>
      </template>
    </el-dialog>
  </el-container>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, nextTick, onBeforeUnmount } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Reading, Plus, Refresh, Document, Collection, Grid, DataAnalysis, Search, View, Delete, MapLocation, Warning, Guide, QuestionFilled, UploadFilled } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import { useKnowledgeStats } from '@/composables/useKnowledgeStats'
import { getCategoryDisplayName, buildPieChartOption, buildLineChartOption } from './KnowledgeBaseChartHelpers'
import { http } from '@/utils/request'

const stats = reactive({
  totalDocuments: 0,
  totalChunks: 0,
  totalVectors: 0,
  categories: 0
})

const filters = reactive({
  category: '',
  keyword: '',
  status: ''
})

const pagination = reactive({
  currentPage: 1,
  pageSize: 10
})

const documents = ref<any[]>([])
const loading = ref(false)

const pieChartRef = ref<HTMLElement>()
const lineChartRef = ref<HTMLElement>()
const chartInstances = ref<echarts.ECharts[]>([])
const { getCategoryDistribution, getSearchTrend } = useKnowledgeStats()

const initCharts = () => {
  // Dispose existing chart instances
  chartInstances.value.forEach((chart) => chart.dispose())
  chartInstances.value = []

  // Pie chart - category distribution
  if (pieChartRef.value) {
    const pieChart = echarts.init(pieChartRef.value)
    chartInstances.value.push(pieChart)
    const data = getCategoryDistribution(filteredDocuments.value).map(
      (entry) => ({ name: getCategoryDisplayName(entry.name), value: entry.value })
    )
    pieChart.setOption(buildPieChartOption(data))
  }

  // Line chart - 7-day search trend
  if (lineChartRef.value) {
    const lineChart = echarts.init(lineChartRef.value)
    chartInstances.value.push(lineChart)
    const trend = getSearchTrend()
    lineChart.setOption(buildLineChartOption(trend))
  }
}

const handleResize = () => {
  chartInstances.value.forEach((chart) => chart.resize())
}

const uploadDialog = reactive({
  visible: false,
  uploading: false
})

const uploadForm = reactive({
  title: '',
  category: '',
  content: '',
  fileType: 'text'
})

const uploadMode = ref<string>('file')
const uploadRef = ref()
const textUploadFormRef = ref()

const uploadRules = {
  title: [
    { required: true, message: '请输入文档标题', trigger: 'blur' },
    { min: 2, max: 200, message: '长度在 2 到 200 个字符', trigger: 'blur' }
  ],
  category: [
    { required: true, message: '请选择文档分类', trigger: 'change' }
  ],
  content: [
    { required: true, message: '请输入文档内容', trigger: 'blur' },
    { min: 10, max: 10000, message: '长度在 10 到 10000 个字符', trigger: 'blur' }
  ]
}

const uploadFormRef = ref()

const viewDialog = reactive({
  visible: false,
  docId: '',
  title: '',
  category: '',
  status: '',
  chunkCount: 0,
  uploadTime: '',
  content: ''
})

const mockDocuments = [
  {
    docId: 'doc-001',
    title: '故宫博物院游览指南',
    category: 'ATTRACTION',
    status: 'COMPLETED',
    chunkCount: 5,
    uploadTime: '2026-04-20T10:00:00',
    content: '故宫博物院位于北京市中心，旧称紫禁城，是中国明清两代的皇家宫殿，也是世界上现存规模最大、保存最完整的木质结构古建筑群。\n\n【开放时间】\n旺季（4月1日-10月31日）：08:30-17:00\n淡季（11月1日-3月31日）：08:30-16:30\n每周一闭馆（法定节假日除外）\n\n【门票价格】\n旺季：60元/人\n淡季：40元/人\n\n【游览路线推荐】\n中轴线：午门→太和殿→中和殿→保和殿→乾清宫→坤宁宫→御花园→神武门\n东线：钟表馆→珍宝馆\n西线：慈宁宫→寿康宫\n\n【注意事项】\n1. 需要提前在网上预约购票\n2. 建议游览时间3-4小时\n3. 请携带有效身份证件'
  },
  {
    docId: 'doc-002',
    title: '张家界国家森林公园介绍',
    category: 'ATTRACTION',
    status: 'COMPLETED',
    chunkCount: 4,
    uploadTime: '2026-04-19T14:30:00',
    content: '张家界国家森林公园位于湖南省张家界市武陵源区，是中国第一个国家森林公园。公园总面积130平方公里，以独特的石英砂岩峰林地貌闻名于世。\n\n【主要景点】\n1. 天子山：可观赏云海、日出、冬雪等壮丽景观\n2. 袁家界：电影《阿凡达》悬浮山取景地\n3. 金鞭溪：全长7.5公里的美丽峡谷\n4. 黄石寨：有"不到黄石寨，枉到张家界"之说\n\n【门票信息】\n成人票：225元/人（含环保车）\n有效期4天，可多次进入\n\n【交通方式】\n飞机：张家界荷花国际机场\n火车：张家界火车站\n景区内乘坐环保车游览'
  },
  {
    docId: 'doc-003',
    title: '旅游景区退改签政策',
    category: 'POLICY',
    status: 'COMPLETED',
    chunkCount: 3,
    uploadTime: '2026-04-18T09:15:00',
    content: '为保障游客权益，规范旅游市场秩序，特制定以下退改签政策：\n\n【退票政策】\n1. 使用日期前3天（含）以上退票：全额退款\n2. 使用日期前1-2天退票：收取10%手续费\n3. 使用日期当天退票：收取30%手续费\n4. 使用后或过期不可退票\n\n【改签政策】\n1. 使用日期前1天（含）以上可免费改签一次\n2. 当天改签需支付票面价格10%的手续费\n3. 每张票仅限改签一次\n\n【特殊情况】\n1. 因天气原因景区关闭，可全额退款\n2. 因不可抗力因素导致无法游览，可全额退款\n3. 如有疑问请拨打客服热线：400-888-8888'
  },
  {
    docId: 'doc-004',
    title: '云南大理-丽江五日游路线',
    category: 'ROUTE',
    status: 'COMPLETED',
    chunkCount: 6,
    uploadTime: '2026-04-17T16:45:00',
    content: '【云南大理-丽江五日精品路线】\n\nDay 1：抵达大理\n- 上午：抵达大理，入住古城客栈\n- 下午：大理古城漫步（五华楼、洋人街、人民路）\n- 晚上：品尝白族特色美食（酸辣鱼、乳扇、饵丝）\n\nDay 2：洱海环游\n- 上午：租车环洱海（推荐电动车或自行车）\n- 中午：双廊古镇午餐\n- 下午：南诏风情岛、小普陀\n- 晚上：返回古城，逛夜市\n\nDay 3：大理→丽江\n- 上午：崇圣寺三塔\n- 下午：乘大巴前往丽江（约3小时）\n- 晚上：丽江古城夜游（四方街、酒吧街）\n\nDay 4：玉龙雪山\n- 全天：玉龙雪山一日游\n  - 乘坐大索道至冰川公园（海拔4506米）\n  - 游览蓝月谷（建议下午去，光线最美）\n  - 观赏《印象丽江》实景演出\n\nDay 5：束河古镇→返程\n- 上午：束河古镇（比丽江古城更安静）\n- 下午：购买特产，返程\n\n【预算参考】\n交通：约1500元\n住宿：约1200元（5晚）\n门票：约800元\n餐饮：约1000元\n总计：约4500元/人'
  },
  {
    docId: 'doc-005',
    title: '旅游常见问题解答',
    category: 'FAQ',
    status: 'COMPLETED',
    chunkCount: 4,
    uploadTime: '2026-04-16T11:20:00',
    content: '【常见问题解答】\n\nQ1：如何预订旅游产品？\nA：您可以在我们的平台上浏览商品列表，选择心仪的旅游产品后点击"立即购买"，按照提示完成支付即可。\n\nQ2：支付方式有哪些？\nA：目前支持支付宝、微信支付和银行卡支付。\n\nQ3：如何查看我的订单？\nA：登录后，点击导航栏中的"我的订单"即可查看所有订单信息。\n\nQ4：门票可以转让给他人使用吗？\nA：部分景点门票实名制，不可转让。非实名制门票可以转让，具体请查看产品详情页说明。\n\nQ5：遇到问题如何联系客服？\nA：您可以拨打客服热线400-888-8888，或通过AI智能助手在线咨询。\n\nQ6：旅游保险怎么购买？\nA：下单时可选择附加旅游意外险，保费10-50元不等，最高赔付金额50万元。'
  },
  {
    docId: 'doc-006',
    title: '黄山风景区攻略',
    category: 'ATTRACTION',
    status: 'COMPLETED',
    chunkCount: 5,
    uploadTime: '2026-04-15T08:00:00',
    content: '黄山位于安徽省黄山市，是中国十大风景名胜之一，以奇松、怪石、云海、温泉"四绝"闻名天下。\n\n【最佳旅游时间】\n3-5月：春暖花开，云海出现概率高\n6-8月：夏季避暑，但游客较多\n9-11月：秋色迷人，最佳摄影季节\n12-2月：冬季雪景，但部分景点关闭\n\n【上山路线】\n前山（慈光阁）：较陡峭，适合体力好的游客\n后山（云谷寺）：较平缓，适合家庭出游\n\n【必看景点】\n迎客松、光明顶、飞来石、莲花峰、天都峰、始信峰\n\n【门票】\n旺季（3-11月）：190元\n淡季（12-2月）：150元\n索道单程：80-90元\n\n【住宿】\n山顶酒店：标间600-1200元/晚\n建议提前预订，旺季一房难求'
  },
  {
    docId: 'doc-007',
    title: '旅游安全须知',
    category: 'POLICY',
    status: 'COMPLETED',
    chunkCount: 3,
    uploadTime: '2026-04-14T13:00:00',
    content: '【旅游安全须知】\n\n一、出行前准备\n1. 了解目的地天气和地形情况\n2. 准备必要的药品和急救包\n3. 购买旅游意外保险\n4. 告知家人行程安排\n\n二、游览中注意事项\n1. 遵守景区规定，不进入未开放区域\n2. 注意防滑、防跌落，特别是山区和水边\n3. 不要攀爬没有安全设施的悬崖或岩石\n4. 保管好个人财物，谨防小偷\n5. 文明旅游，不乱扔垃圾\n\n三、紧急情况处理\n1. 拨打景区救援电话\n2. 拨打110报警、120急救\n3. 联系旅行社或平台客服\n4. 保留证据，便于后续理赔'
  },
  {
    docId: 'doc-008',
    title: '三亚海滨度假路线推荐',
    category: 'ROUTE',
    status: 'COMPLETED',
    chunkCount: 4,
    uploadTime: '2026-04-13T10:30:00',
    content: '【三亚四日海滨度假路线】\n\nDay 1：亚龙湾\n- 上午：抵达三亚，入住亚龙湾酒店\n- 下午：亚龙湾海滩休闲，体验水上项目\n- 晚上：亚龙湾热带天堂森林公园夜景\n\nDay 2：南山文化旅游区\n- 上午：南山寺、海上观音像\n- 下午：天涯海角游览区\n- 晚上：第一市场品尝海鲜\n\nDay 3：蜈支洲岛\n- 全天：蜈支洲岛一日游\n  - 潜水、浮潜体验\n  - 环岛观光车\n  - 情人桥、观日岩\n\nDay 4：免税购物→返程\n- 上午：海棠湾免税店购物\n- 下午：返程\n\n【预算参考】\n机票：约1500-3000元（看出发地）\n住宿：约800-2000元/晚\n餐饮：约200-400元/天\n门票+活动：约1000元\n总计：约6000-10000元/人'
  }
]

const filteredDocuments = computed(() => {
  let result = [...documents.value]

  if (filters.category) {
    result = result.filter((doc: any) => doc.category === filters.category)
  }

  if (filters.status) {
    result = result.filter((doc: any) => doc.status === filters.status)
  }

  if (filters.keyword) {
    const keyword = filters.keyword.toLowerCase()
    result = result.filter((doc: any) =>
      doc.title.toLowerCase().includes(keyword)
    )
  }

  return result
})

const paginatedDocuments = computed(() => {
  const start = (pagination.currentPage - 1) * pagination.pageSize
  const end = start + pagination.pageSize
  return filteredDocuments.value.slice(start, end)
})

const loadDocuments = async () => {
  loading.value = true
  try {
    const data = await http.get('/api/knowledge/documents')
    if (data && Array.isArray(data) && data.length > 0) {
      documents.value = data
    } else {
      documents.value = [...mockDocuments]
    }
  } catch {
    documents.value = [...mockDocuments]
  } finally {
    loading.value = false
    updateStats()
    nextTick(() => initCharts())
  }
}

const updateStats = () => {
  stats.totalDocuments = documents.value.length
  stats.totalChunks = documents.value.reduce((sum: number, doc: any) => sum + (doc.chunkCount || 0), 0)
  stats.totalVectors = stats.totalChunks
  const categories = new Set(documents.value.map((doc: any) => doc.category))
  stats.categories = categories.size
}

const handleFilterChange = () => {
  pagination.currentPage = 1
}

const handleSearch = () => {
  pagination.currentPage = 1
}

const showUploadDialog = () => {
  uploadDialog.visible = true
  uploadMode.value = 'file'
  uploadForm.title = ''
  uploadForm.category = ''
  uploadForm.content = ''
  uploadForm.fileType = 'text'
}

const beforeFileUpload = (file: File) => {
  const ext = file.name.toLowerCase()
  if (!ext.endsWith('.pdf') && !ext.endsWith('.txt')) {
    ElMessage.error('仅支持PDF和TXT格式')
    return false
  }
  if (file.size > 10 * 1024 * 1024) {
    ElMessage.error('文件大小不能超过10MB')
    return false
  }
  return true
}

const handleExceed = () => {
  ElMessage.warning('只能上传一个文件，请先移除已选文件')
}

const submitUpload = async () => {
  try {
    uploadDialog.uploading = true

    if (uploadMode.value === 'file') {
      // File upload mode
      const fileList = uploadRef.value?.uploadFiles
      if (!fileList || fileList.length === 0) {
        ElMessage.warning('请先选择文件')
        return
      }

      const formData = new FormData()
      formData.append('file', fileList[0].raw)
      if (uploadForm.title) {
        formData.append('title', uploadForm.title)
      }
      if (uploadForm.category) {
        formData.append('category', uploadForm.category)
      }
      formData.append('userId', '1')

      await http.post('/api/knowledge/upload/file', formData, {
        headers: { 'Content-Type': 'multipart/form-data' }
      })

      ElMessage.success('文件上传成功')
      uploadDialog.visible = false
      await loadDocuments()
    } else {
      // Text upload mode (original)
      await textUploadFormRef.value?.validate()

      await http.post('/api/knowledge/upload', {
        title: uploadForm.title,
        category: uploadForm.category,
        content: uploadForm.content,
        fileType: uploadForm.fileType,
        userId: 1
      })

      ElMessage.success('文档上传成功')
      uploadDialog.visible = false
      await loadDocuments()
    }
  } catch (error: any) {
    if (error?.message?.includes('validate')) return

    // Fallback: add document locally
    if (uploadMode.value === 'text') {
      const newDoc = {
        docId: 'doc-' + Date.now(),
        title: uploadForm.title,
        category: uploadForm.category,
        status: 'COMPLETED',
        chunkCount: Math.ceil(uploadForm.content.length / 500),
        uploadTime: new Date().toISOString(),
        content: uploadForm.content
      }
      documents.value = [newDoc, ...documents.value]
      updateStats()

      ElMessage.success('文档已添加（本地模式）')
      uploadDialog.visible = false
    } else {
      ElMessage.error('文件上传失败: ' + (error?.message || '未知错误'))
    }
  } finally {
    uploadDialog.uploading = false
  }
}

const viewDocument = (doc: any) => {
  viewDialog.docId = doc.docId
  viewDialog.title = doc.title
  viewDialog.category = doc.category
  viewDialog.status = doc.status
  viewDialog.chunkCount = doc.chunkCount
  viewDialog.uploadTime = doc.uploadTime
  viewDialog.content = doc.content || `${doc.title}\n\n该文档包含 ${doc.chunkCount || 0} 个文本分块，已成功向量化并存储。可通过AI助手进行智能检索和问答。`
  viewDialog.visible = true
}

const deleteDocument = async (doc: any) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除文档"${doc.title}"吗？删除后将无法恢复。`,
      '确认删除',
      { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }
    )

    try {
      await http.delete(`/api/knowledge/document/${doc.docId}`)
      ElMessage.success('文档删除成功')
    } catch {
      documents.value = documents.value.filter((d: any) => d.docId !== doc.docId)
      updateStats()
      ElMessage.success('文档已删除（本地模式）')
    }

    await loadDocuments()
  } catch (error) {
    // cancelled
  }
}

const handleSizeChange = (val: number) => {
  pagination.pageSize = val
  pagination.currentPage = 1
}

const handleCurrentChange = (val: number) => {
  pagination.currentPage = val
}

const getCategoryName = (code: string) => {
  const map: Record<string, string> = {
    'ATTRACTION': '景点介绍',
    'POLICY': '政策说明',
    'ROUTE': '路线推荐',
    'FAQ': '常见问题'
  }
  return map[code] || code
}

const getCategoryTagType = (code: string) => {
  const map: Record<string, string> = {
    'ATTRACTION': 'success',
    'POLICY': 'warning',
    'ROUTE': '',
    'FAQ': 'danger'
  }
  return map[code] || 'info'
}

const getCategoryColor = (code: string) => {
  const map: Record<string, string> = {
    'ATTRACTION': '#67C23A',
    'POLICY': '#E6A23C',
    'ROUTE': '#409EFF',
    'FAQ': '#F56C6C'
  }
  return map[code] || '#909399'
}

const getCategoryIcon = (code: string) => {
  const map: Record<string, string> = {
    'ATTRACTION': 'MapLocation',
    'POLICY': 'Warning',
    'ROUTE': 'Guide',
    'FAQ': 'QuestionFilled'
  }
  return map[code] || 'Document'
}

const getStatusName = (code: string) => {
  const map: Record<string, string> = {
    'PROCESSING': '处理中',
    'COMPLETED': '已完成',
    'FAILED': '失败'
  }
  return map[code] || code
}

const getStatusTagType = (code: string) => {
  const map: Record<string, string> = {
    'PROCESSING': 'warning',
    'COMPLETED': 'success',
    'FAILED': 'danger'
  }
  return map[code] || ''
}

const formatDateTime = (dateStr: string) => {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleString('zh-CN')
}

onMounted(() => {
  loadDocuments()
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  chartInstances.value.forEach((chart) => chart.dispose())
  chartInstances.value = []
})
</script>

<style scoped>
.knowledge-base-container {
  min-height: 100vh;
  background: #f5f7fa;
}

.page-header {
  background: white;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  padding: 0;
}

.header-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 100%;
  max-width: 1400px;
  margin: 0 auto;
  padding: 0 20px;
}

.title-section {
  display: flex;
  align-items: center;
  gap: 12px;
}

.page-title {
  font-size: 24px;
  font-weight: bold;
  color: #303133;
}

.action-section {
  display: flex;
  gap: 12px;
}

.main-content {
  max-width: 1400px;
  margin: 0 auto;
  padding: 20px;
}

.stats-row {
  margin-bottom: 20px;
}

.stat-card {
  text-align: center;
  transition: transform 0.2s;
}

.stat-card:hover {
  transform: translateY(-4px);
}

.stat-content {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
  padding: 10px;
}

.stat-info {
  text-align: left;
}

.stat-value {
  font-size: 28px;
  font-weight: bold;
  color: #409eff;
  line-height: 1;
}

.stat-label {
  font-size: 14px;
  color: #909399;
  margin-top: 4px;
}

.search-card {
  margin-bottom: 20px;
}

.chart-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
  margin: 20px 0;
}

.chart-card {
  min-height: 380px;
}

.documents-card {
  min-height: 400px;
}

.title-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.document-title {
  max-width: 260px;
  font-weight: 500;
}

.pagination-container {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}

.document-content {
  background: #f5f7fa;
  padding: 16px;
  border-radius: 8px;
  max-height: 400px;
  overflow-y: auto;
}

.content-text {
  line-height: 1.8;
  color: #606266;
  white-space: pre-wrap;
  margin: 0;
  font-size: 14px;
}

.upload-area {
  width: 100%;
}

.upload-area :deep(.el-upload) {
  width: 100%;
}

.upload-area :deep(.el-upload-dragger) {
  width: 100%;
  padding: 30px;
}

.upload-tabs :deep(.el-tabs__header) {
  margin-bottom: 16px;
}

@media (max-width: 768px) {
  .chart-row {
    grid-template-columns: 1fr;
  }

  .stats-row {
    :deep(.el-col) {
      margin-bottom: 10px;
    }
  }

  .header-content {
    flex-direction: column;
    gap: 12px;
    padding: 12px;
  }

  .action-section {
    width: 100%;
  }

  .action-section .el-button {
    flex: 1;
  }
}
</style>
