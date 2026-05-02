# Tasks: 论文核心功能补全实施清单（TDD模式）

## 前置准备: 测试基础设施搭建

### Task 0.1: Java后端测试基础设施
- [ ] 0.1.1 所有模块 pom.xml 添加 `spring-boot-starter-test` 依赖
- [ ] 0.1.2 创建 `src/test/resources/application-test.yml` 测试配置（禁用外部API、使用H2内存库）
- [ ] 0.1.3 创建测试基类 `BaseServiceTest`（通用Mock配置）
- [ ] 0.1.4 验证 `mvn test` 可正常运行

### Task 0.2: Vue前端测试基础设施
- [ ] 0.2.1 安装测试依赖: `vitest` + `@vue/test-utils` + `happy-dom` + `@pinia/testing`
- [ ] 0.2.2 创建 `vitest.config.ts` 配置文件
- [ ] 0.2.3 创建 `src/test/setup.ts` 测试初始化文件
- [ ] 0.2.4 package.json 添加 `"test": "vitest"` 脚本
- [ ] 0.2.5 验证 `npm test` 可正常运行

---

## Phase 1: P0 核心创新功能（答辩必备）

### Task 1.1: RAG管道接入聊天流程

#### RED - 先写失败测试
- [ ] 1.1.T1 `DynamicThresholdServiceTest`: 测试不同意图类型返回不同阈值
- [ ] 1.1.T2 `AIServiceImplTest.testChatWithRAG_FactQuery`: 事实性查询触发RAG检索
- [ ] 1.1.T3 `AIServiceImplTest.testChatWithRAG_Greeting`: 问候语跳过RAG直接回复
- [ ] 1.1.T4 `AIServiceImplTest.testChatWithRAG_RecommendQuery`: 推荐性查询使用低阈值高召回
- [ ] 1.1.T5 `AIServiceImplTest.testChatWithRAG_SourceReturned`: RAG回复包含来源信息
- [ ] 1.1.T6 `AIChatControllerTest.testSendMessage_FactIntent`: Controller根据意图路由到正确处理

#### GREEN - 最小实现
- [ ] 1.1.1 `ChatResponse` DTO 增加 `sources` 字段（来源文档列表）
- [ ] 1.1.2 `AIServiceImpl` 注入 `KnowledgeBaseService` + `IntentService`
- [ ] 1.1.3 新增 `chatWithRAG()` 方法：意图识别 → 动态阈值 → RAG检索 → 增强上下文 → LLM生成
- [ ] 1.1.4 改造 `AIChatController.sendMessage()` 根据意图走不同路径
- [ ] 1.1.5 非RAG路径（GREETING/通用闲聊）保持本地回复

#### REFACTOR
- [ ] 1.1.R1 提取 `ChatPipeline` 接口，封装完整的对话处理流程

#### 前端测试 + 实现
- [ ] 1.1.F1 `AIChatView.test.ts`: 测试来源标注组件正确渲染
- [ ] 1.1.F2 前端 `AIChatView.vue` 展示来源标注信息

---

### Task 1.2: 动态阈值机制

#### RED - 先写失败测试
- [ ] 1.2.T1 `DynamicThresholdServiceTest.testFactQueryThreshold`: PRICE_INQUIRY → 0.5
- [ ] 1.2.T2 `DynamicThresholdServiceTest.testRecommendQueryThreshold`: PRODUCT_RECOMMENDATION → 0.3
- [ ] 1.2.T3 `DynamicThresholdServiceTest.testOpenQueryThreshold`: GENERAL → 0.2
- [ ] 1.2.T4 `DynamicThresholdServiceTest.testGreetingNoThreshold`: GREETING → 不需要阈值（返回null或特殊值）
- [ ] 1.2.T5 `DynamicThresholdServiceTest.testPolicyThreshold`: POLICY_INQUIRY → 0.4
- [ ] 1.2.T6 `DynamicThresholdServiceTest.testUnknownIntent`: UNKNOWN → 默认0.3

#### GREEN - 最小实现
- [ ] 1.2.1 新建 `DynamicThresholdService` 类
- [ ] 1.2.2 定义 IntentType → 阈值映射
- [ ] 1.2.3 在RAG检索前调用动态阈值
- [ ] 1.2.4 `RAGSearchRequest` 支持动态阈值参数

---

### Task 1.3: 增强型上下文接入

#### RED - 先写失败测试
- [ ] 1.3.T1 `EnhancedContextTest.testBuildContext_SingleSource`: 单来源标注格式正确
- [ ] 1.3.T2 `EnhancedContextTest.testBuildContext_MultipleSources`: 多来源按相似度排序
- [ ] 1.3.T3 `EnhancedContextTest.testBuildContext_EmptyResults`: 空结果返回友好提示
- [ ] 1.3.T4 `EnhancedContextTest.testPromptTemplate_ContainsSourceRequirement`: prompt模板包含来源标注要求

#### GREEN - 最小实现
- [ ] 1.3.1 复用 `buildEnhancedContext()` 构建带来源的prompt
- [ ] 1.3.2 设计提示词模板要求LLM标注来源
- [ ] 1.3.3 `ChatResponse` 返回来源列表给前端

---

### Task 1.4: SSE流式输出

#### RED - 先写失败测试
- [ ] 1.4.T1 `SSEChatControllerTest.testStreamEndpoint_ReturnsSSE`: 端点返回 text/event-stream
- [ ] 1.4.T2 `SSEChatControllerTest.testStreamEndpoint_ChunkedOutput`: 收到多个data事件
- [ ] 1.4.T3 `SSEChatControllerTest.testStreamEndpoint_Heartbeat`: 30秒内发送心跳注释
- [ ] 1.4.T4 `SSEChatControllerTest.testStreamEndpoint_CompletionEvent`: 结束时发送[DONE]标记

#### GREEN - 最小实现
- [ ] 1.4.1 后端新增 `GET /api/ai/chat/stream` 端点（SseEmitter）
- [ ] 1.4.2 实现字符chunk级流式推送
- [ ] 1.4.3 添加30秒心跳防止超时

#### 前端测试 + 实现
- [ ] 1.4.F1 `useStreamChat.test.ts`: 测试流式接收和消息拼接
- [ ] 1.4.F2 前端 `AIChatView.vue` 改用 `fetch + ReadableStream` 接收
- [ ] 1.4.F3 实时追加AI回复 + 打字动画效果
- [ ] 1.4.F4 Vite proxy 添加 `/api/ai/chat/stream` 代理规则

---

## Phase 2: P1 关键功能（完整闭环）

### Task 2.1: Milvus向量数据库接入

#### RED - 先写失败测试
- [ ] 2.1.T1 `MilvusVectorStoreTest.testUpsert`: 插入向量后可检索到
- [ ] 2.1.T2 `MilvusVectorStoreTest.testSearch_TopK`: TopK参数限制返回数量
- [ ] 2.1.T3 `MilvusVectorStoreTest.testSearch_WithThreshold`: 阈值过滤低相似度结果
- [ ] 2.1.T4 `MilvusVectorStoreTest.testDelete`: 删除后检索不到
- [ ] 2.1.T5 `MilvusVectorStoreTest.testConnectionFailure_Fallback`: 连接失败降级到内存
- [ ] 2.1.T6 `KnowledgeBaseServiceTest.testUploadAndSearch`: 完整上传→检索流程

#### GREEN - 最小实现
- [ ] 2.1.1 新建 `MilvusConfig` 配置类，注入 `MilvusClient` Bean
- [ ] 2.1.2 新建 `MilvusVectorStore` 实现向量CRUD
- [ ] 2.1.3 改造 `KnowledgeBaseServiceSimpleImpl` 使用 Milvus 替代 HashMap
- [ ] 2.1.4 添加降级策略：Milvus不可用时回退内存存储

---

### Task 2.2: 智能路由外部服务调用

#### RED - 先写失败测试
- [ ] 2.2.T1 `ExternalServiceClientTest.testGetProducts_Success`: 成功调用产品服务返回列表
- [ ] 2.2.T2 `ExternalServiceClientTest.testGetProducts_Timeout`: 超时降级到本地回复
- [ ] 2.2.T3 `ExternalServiceClientTest.testGetOrders_Success`: 成功调用订单服务
- [ ] 2.2.T4 `ExternalServiceClientTest.testGetOrders_Unauthorized`: 401降级处理
- [ ] 2.2.T5 `AIChatControllerTest.testProductRecommendation_ReturnsRealData`: 路由返回真实产品
- [ ] 2.2.T6 `AIChatControllerTest.testOrderQuery_ReturnsRealData`: 路由返回真实订单

#### GREEN - 最小实现
- [ ] 2.2.1 新建 `ExternalServiceClient` 封装 RestTemplate 调用
- [ ] 2.2.2 实现产品推荐调用 `http://localhost:8082/api/products`
- [ ] 2.2.3 实现订单查询调用 `http://localhost:8083/api/orders`
- [ ] 2.2.4 替换 `handleProductRecommendation()` 硬编码
- [ ] 2.2.5 替换 `handleOrderQuery()` 硬编码
- [ ] 2.2.6 添加降级：服务不可用时返回本地知识回复

---

### Task 2.3: Markdown渲染 + 来源标注UI

#### RED - 先写失败测试
- [ ] 2.3.T1 `useMarkdown.test.ts`: 测试列表/代码块/链接正确渲染
- [ ] 2.3.T2 `useMarkdown.test.ts`: 测试XSS过滤（script标签被转义）
- [ ] 2.3.T3 `SourceBadge.test.ts`: 测试来源标注渲染（名称+相似度）
- [ ] 2.3.T4 `SourceBadge.test.ts`: 测试无来源时不渲染

#### GREEN - 最小实现
- [ ] 2.3.1 安装 `marked` + `highlight.js` + `dompurify` 前端依赖
- [ ] 2.3.2 新建 `useMarkdown` composable 封装渲染逻辑
- [ ] 2.3.3 AI消息气泡使用 `v-html` 渲染Markdown
- [ ] 2.3.4 新建 `<SourceBadge>` 来源标注组件
- [ ] 2.3.5 在AI回复末尾展示来源文档列表

---

## Phase 3: P2 体验增强（锦上添花）

### Task 3.1: ECharts知识库统计图表

#### RED - 先写失败测试
- [ ] 3.1.T1 `useKnowledgeStats.test.ts`: 测试统计数据计算逻辑
- [ ] 3.1.T2 `KnowledgeBaseView.test.ts`: 测试饼图/折线图组件挂载

#### GREEN - 最小实现
- [ ] 3.1.1 安装 `echarts` + `vue-echarts` 前端依赖
- [ ] 3.1.2 `KnowledgeBaseView.vue` 添加类别分布饼图
- [ ] 3.1.3 添加检索次数趋势折线图（模拟7天数据）

---

### Task 3.2: PDF/TXT文件上传

#### RED - 先写失败测试
- [ ] 3.2.T1 `FileParserTest.testParsePdf_Success`: PDF文件正确提取文本
- [ ] 3.2.T2 `FileParserTest.testParsePdf_EmptyFile`: 空文件抛出异常
- [ ] 3.2.T3 `FileParserTest.testParseTxt_Success`: TXT文件正确读取
- [ ] 3.2.T4 `FileParserTest.testParse_UnsupportedFormat`: 不支持的格式提示错误
- [ ] 3.2.T5 `FileParserTest.testParse_FileTooLarge`: 超过10MB提示错误

#### GREEN - 最小实现
- [ ] 3.2.1 后端新增 `POST /api/knowledge/upload/file` MultipartFile端点
- [ ] 3.2.2 使用Apache Tika解析PDF提取文本
- [ ] 3.2.3 前端 `KnowledgeBaseView.vue` 改用 `el-upload` 拖拽上传
- [ ] 3.2.4 上传进度显示 + 成功后刷新文档列表

---

### Task 3.3: 对话摘要压缩

#### RED - 先写失败测试
- [ ] 3.3.T1 `ContextServiceTest.testCompressHistory_Over10Rounds`: 超过10轮触发摘要
- [ ] 3.3.T2 `ContextServiceTest.testCompressHistory_Under10Rounds`: 不超过10轮不压缩
- [ ] 3.3.T3 `ContextServiceTest.testCompressHistory_SummaryQuality`: 摘要保留关键实体
- [ ] 3.3.T4 `ContextServiceTest.testCompressHistory_FallbackOnLLMFailure`: LLM失败降级到首句提取

#### GREEN - 最小实现
- [ ] 3.3.1 `conversation_session` 表增加 `summary` 字段
- [ ] 3.3.2 新增摘要生成逻辑（超过10轮时触发）
- [ ] 3.3.3 上下文窗口 = 摘要 + 最近5轮对话
- [ ] 3.3.4 调用LLM生成摘要（降级：提取首句）

---

## 验收标准

### 测试覆盖率目标
- Java后端核心服务: **≥80%**
- Vue前端核心组件: **≥70%**
- 所有测试必须 `mvn test` / `npm test` 通过

### Phase 1 验收
- [ ] `mvn test -pl travel-ai` 全部通过（DynamicThreshold + RAG Pipeline + SSE）
- [ ] `npm test` 前端测试通过（来源标注 + 流式接收）
- [ ] 发送"故宫门票多少钱" → 走RAG检索 → 回复包含来源标注
- [ ] 发送"你好" → 走本地回复（不调用RAG）
- [ ] AI回复逐步显示（SSE流式）
- [ ] 动态阈值：事实查询用0.5，推荐查询用0.3

### Phase 2 验收
- [ ] Milvus相关测试全部通过（Mock Milvus Client）
- [ ] 外部服务调用测试通过（Mock RestTemplate）
- [ ] Markdown渲染测试覆盖XSS防护
- [ ] Milvus存储向量，重启后向量不丢失
- [ ] AI回复Markdown正确渲染（列表、代码块、链接）

### Phase 3 验收
- [ ] 文件解析测试覆盖PDF/TXT/异常场景
- [ ] 对话压缩测试覆盖10轮边界条件
- [ ] 知识库页面显示饼图和折线图
- [ ] 可上传PDF文件并自动解析入库
- [ ] 超过10轮对话后自动生成摘要
