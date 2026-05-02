# Design: 论文核心功能补全 - 技术方案

## 现有架构分析

### 已有组件（可复用）
- `KnowledgeBaseServiceSimpleImpl`: 向量搜索、增强上下文构建、文档管理
- `TextChunker`: 语义分块算法（已完整实现）
- `IntentServiceImpl`: 9种意图识别 + 实体提取 + 路由生成
- `ContextServiceImpl`: Redis缓存 + MySQL持久化的对话历史
- `GLM5Config`: ChatLanguageModel + EmbeddingModel Bean
- `AIChatController`: 聊天入口，已有意图识别和路由框架

### 关键断点
1. `AIChatController.sendMessage()` → 直接调用 `aiService.chatStream()`，未经过RAG
2. `AIServiceImpl` 无 RAG 依赖注入
3. 前端 `AIChatView.vue` 用HTTP POST，无SSE监听

---

## Phase 1 技术方案

### 1. RAG管道接入聊天流程

**方案**: 改造 `AIChatController` 和 `AIServiceImpl`，在对话流程中加入RAG检索

```
用户消息 → 意图识别 → 判断是否需要RAG
  ├─ GREETING/OTHER → 直接LLM/本地回复
  └─ 需要 RAG → 查询预处理 → 动态阈值 → 向量检索 → 增强型上下文 → LLM生成
```

**关键改动**:
- `AIServiceImpl` 注入 `KnowledgeBaseService` 和 `IntentService`
- 新增 `chatWithRAG(String message, String sessionId)` 方法
- `AIChatController.sendMessage()` 根据意图选择处理路径
- `ChatResponse` DTO 增加 `sources` 字段（来源信息）

### 2. 动态阈值机制

**方案**: 新增 `DynamicThresholdService`

```java
public class DynamicThresholdService {
    // 意图 → 阈值映射
    Map<IntentType, Double> thresholdMap = Map.of(
        PRICE_INQUIRY, 0.5,      // 事实性：高精度
        ATTRACTION_QUERY, 0.5,   // 事实性：高精度
        PRODUCT_RECOMMENDATION, 0.3, // 推荐性：中等
        POLICY_INQUIRY, 0.4,     // 中间型
        GENERAL, 0.2             // 开放性：高召回
    );
}
```

**关键改动**:
- 新建 `DynamicThresholdService` 类
- 在RAG检索前调用 `getThreshold(intent)` 获取动态阈值
- 搜索请求使用动态阈值替代固定0.6

### 3. 增强型上下文接入

**方案**: 复用已有 `buildEnhancedContext()`，接入聊天流程

**关键改动**:
- RAG检索后调用 `buildEnhancedContext(searchResults)`
- 构造带来源信息的prompt发送给LLM
- `ChatResponse` 增加 `List<SourceInfo> sources` 字段
- prompt模板要求LLM在回答中标注来源

### 4. SSE流式输出

**方案**: 后端 `SseEmitter` + 前端 `EventSource`

**后端**:
- 新增 `GET /api/ai/chat/stream` 端点返回 `SseEmitter`
- 逐token发送（或按字符chunk模拟流式）
- 保持心跳防止超时

**前端**:
- `AIChatView.vue` 用 `fetch + ReadableStream` 替换 axios POST
- 逐步追加AI回复内容
- 显示打字动画效果

---

## Phase 2 技术方案

### 5. Milvus接入

**方案**: 新建 `MilvusVectorStore` 实现 `VectorStore` 接口

- 连接 Milvus (localhost:19530)
- 创建 collection `travel_knowledge` (dimension=768)
- upsert / search / delete 操作
- 替换 `KnowledgeBaseServiceSimpleImpl` 中的 HashMap

### 6. 外部服务调用

**方案**: 使用 `RestTemplate` 调用其他微服务

- 产品服务: `http://localhost:8082/api/products`
- 订单服务: `http://localhost:8083/api/orders`
- 替换 `handleProductRecommendation()` 和 `handleOrderQuery()` 中的硬编码

### 7. Markdown渲染 + 来源标注

**前端**:
- 安装 `marked` + `highlight.js`
- 消息气泡使用 `v-html="renderMarkdown(content)"`
- 来源标注组件 `<SourceBadge>`

---

## Phase 3 技术方案

### 8. ECharts图表
- 安装 `echarts` 依赖
- `KnowledgeBaseView.vue` 添加类别饼图 + 检索趋势折线图

### 9. PDF/TXT文件上传
- 后端使用 Apache Tika（已在pom.xml）解析PDF
- 新增 `MultipartFile` 上传端点
- 前端使用 `el-upload` 的拖拽上传

### 10. 对话摘要压缩
- 超过10轮时调用LLM生成摘要
- 摘要存入 `conversation_session.summary` 字段
- 上下文窗口 = 摘要 + 最近5轮完整对话

---

## TDD测试策略

### 测试基础设施

#### Java后端
- **框架**: Spring Boot Starter Test (JUnit 5 + Mockito + AssertJ)
- **配置**: `application-test.yml` 禁用外部API调用，Mock所有外部依赖
- **Mock策略**:
  - `ChatLanguageModel`: Mock，不调用真实GLM API
  - `EmbeddingModel`: Mock，返回固定向量
  - `MilvusClient`: Mock，不连接真实Milvus
  - `RestTemplate`: Mock，不调用真实微服务
  - `RedisTemplate`: Mock 或使用嵌入式Redis

#### Vue前端
- **框架**: Vitest + @vue/test-utils + happy-dom
- **配置**: vitest.config.ts，模拟 Element Plus 组件
- **Mock策略**:
  - API调用: `vi.mock('@/utils/request')` 返回固定数据
  - Pinia Store: `@pinia/testing` 的 `createTestingPinia`
  - Router: `vue-router/mock`
  - fetch/ReadableStream: Mock SSE流式响应

### 测试文件结构

```
# Java
travel-ai/src/test/java/com/travel/ai/
├── service/
│   ├── AIServiceImplTest.java          # RAG管道 + 意图路由
│   ├── DynamicThresholdServiceTest.java # 动态阈值
│   ├── ContextServiceImplTest.java      # 对话历史 + 压缩
│   └── KnowledgeBaseServiceTest.java    # 知识库CRUD + 检索
├── controller/
│   ├── AIChatControllerTest.java       # 聊天端点
│   └── SSEChatControllerTest.java      # SSE流式端点
├── client/
│   ├── MilvusVectorStoreTest.java      # Milvus操作
│   └── ExternalServiceClientTest.java  # 外部服务调用
└── util/
    └── FileParserTest.java             # PDF/TXT解析

# Vue
travel-assistant-web/src/
├── composables/
│   ├── __tests__/
│   │   ├── useMarkdown.test.ts         # Markdown渲染 + XSS
│   │   └── useStreamChat.test.ts       # SSE流式接收
├── components/
│   └── __tests__/
│       ├── SourceBadge.test.ts          # 来源标注组件
│       └── AIChatView.test.ts          # 聊天页面
└── views/
    └── __tests__/
        └── KnowledgeBaseView.test.ts    # 知识库页面
```

### TDD执行流程

每个Task严格遵循 **RED → GREEN → REFACTOR** 循环：

```
1. RED:    写测试 → 确认编译失败（方法/类不存在）
2. GREEN:  最小实现 → 确认测试通过
3. REFACTOR: 重构 → 确认测试仍然通过
4. 重复下一个测试用例
```

### 关键测试场景

| 功能 | 关键测试 | 覆盖目标 |
|------|---------|---------|
| 动态阈值 | 6种意图类型 + 边界值 | 100% |
| RAG管道 | 意图路由 + 检索 + 来源 | ≥85% |
| SSE流式 | 端点 + 心跳 + 结束标记 | ≥80% |
| Milvus | CRUD + 降级 | ≥80% |
| 外部调用 | 成功 + 超时 + 降级 | ≥85% |
| Markdown | 渲染 + XSS防护 | ≥80% |
| 文件上传 | PDF/TXT/异常 | ≥85% |
| 对话压缩 | 10轮边界 + LLM降级 | ≥80% |

---

## 风险评估

| 风险 | 影响 | 缓解措施 |
|------|------|---------|
| GLM API余额不足 | Phase 1 RAG无法调用LLM | 本地fallback保持可用 |
| Milvus未启动 | Phase 2 向量存储失败 | 降级到内存存储 |
| 微服务未启动 | Phase 2 外部调用失败 | 降级到本地知识回复 |
| SSE浏览器兼容 | Phase 1 流式体验 | 降级到普通HTTP响应 |
