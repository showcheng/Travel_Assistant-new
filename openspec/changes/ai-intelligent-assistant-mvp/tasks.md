# AI智能助手 - 详细任务清单

## 📊 任务统计

- **总任务数**: 35
- **预计总工时**: 30人天
- **预计完成时间**: 5周
- **当前进度**: 0/35 (0%)

---

## Phase 1: 基础对话能力 (Week 1-2)

### Week 1: 基础设施搭建

#### Task 1.1: 创建AI服务模块
**负责人**: 后端开发  
**优先级**: P0 (最高)  
**预计时间**: 4小时  
**依赖**: 无

**子任务**:
- [ ] 在travel-assistant父项目下创建travel-ai模块
- [ ] 配置pom.xml，添加必要依赖
- [ ] 配置application.yml基础配置
- [ ] 创建标准包结构 (controller, service, entity, config)
- [ ] 创建AIServiceApplication启动类
- [ ] 配置日志框架
- [ ] 验证模块可正常启动

**验收标准**:
- ✅ 模块可以独立启动
- ✅ 日志正常输出
- ✅ 健康检查接口可访问

**相关文件**:
- `travel-assistant/travel-ai/pom.xml`
- `travel-assistant/travel-ai/src/main/resources/application.yml`
- `travel-assistant/travel-ai/src/main/java/com/travel/ai/AIServiceApplication.java`

---

#### Task 1.2: 集成LangChain4j框架
**负责人**: 后端开发  
**优先级**: P0  
**预计时间**: 6小时  
**依赖**: Task 1.1

**子任务**:
- [ ] 添加LangChain4j依赖到pom.xml
- [ ] 添加Spring AI starter依赖
- [ ] 配置Embedding模型 (使用开源模型或GLM-5 embedding)
- [ ] 创建LLM服务配置类
- [ ] 创建AI服务基础接口
- [ ] 编写简单的对话测试用例
- [ ] 验证模型调用正常

**验收标准**:
- ✅ 可以调用GLM-5生成回复
- ✅ Embedding服务正常工作
- ✅ 错误处理完善

**相关文件**:
- `travel-assistant/travel-ai/pom.xml`
- `travel-assistant/travel-ai/src/main/java/com/travel/ai/config/LangChain4jConfig.java`
- `travel-assistant/travel-ai/src/main/java/com/travel/ai/service/GLM5Service.java`

---

#### Task 1.3: 配置GLM-5 API连接
**负责人**: 后端开发  
**优先级**: P0  
**预计时间**: 4小时  
**依赖**: Task 1.2

**子任务**:
- [ ] 配置GLM-5 API密钥
- [ ] 创建GLM-5客户端配置
- [ ] 实现API重试机制
- [ ] 实现降级策略
- [ ] 编写API连通性测试
- [ ] 测试Token计费功能
- [ ] 验证响应时间符合预期

**验收标准**:
- ✅ API调用成功率 > 99%
- ✅ 重试机制正常工作
- ✅ 降级策略测试通过

**相关配置**:
```yaml
glm5:
  api-key: ${GLM5_API_KEY}
  model: glm-4-flash
  temperature: 0.7
  max-tokens: 2000
  timeout: 30000
  retry:
    max-attempts: 3
    backoff-delay: 1000
```

---

#### Task 1.4: 实现简单对话API
**负责人**: 后端开发  
**优先级**: P0  
**预计时间**: 6小时  
**依赖**: Task 1.3

**子任务**:
- [ ] 创建对话Controller
- [ ] 实现基础对话接口
- [ ] 实现消息格式化
- [ ] 实现错误处理
- [ ] 编写接口文档
- [ ] 编写单元测试
- [ ] 手动测试对话效果

**验收标准**:
- ✅ POST /api/ai/chat/send 接口可用
- ✅ 返回格式符合规范
- ✅ 错误处理完善

**API示例**:
```http
POST /api/ai/chat/send
Content-Type: application/json

{
  "message": "你好",
  "sessionId": "optional"
}

Response:
{
  "sessionId": "session_123",
  "response": "您好！我是智慧旅游助手...",
  "timestamp": "2026-04-16T14:30:00"
}
```

---

#### Task 1.5: 部署Milvus向量数据库
**负责人**: 后端开发  
**优先级**: P1  
**预计时间**: 4小时  
**依赖**: 无

**子任务**:
- [ ] 编写Docker Compose配置文件
- [ ] 配置Milvus单机版
- [ ] 配置Etcd依赖服务
- [ ] 配置数据持久化
- [ ] 启动Milvus服务
- [ ] 创建测试Collection
- [ ] 测试向量存储和检索

**验收标准**:
- ✅ Milvus服务正常启动
- ✅ 可以创建Collection
- ✅ 向量插入和检索正常

**Docker Compose配置**:
```yaml
version: '3.5'
services:
  etcd:
    image: quay.io/coreos/etcd:v3.5.5
    environment:
      - ETCD_AUTO_COMPACTION_MODE=revision
      - ETCD_AUTO_COMPACTION_RETENTION=1000
    volumes:
      - milvus-etcd:/etcd

  minio:
    image: minio/minio:RELEASE.2023-03-20T20-16-18Z
    environment:
      MINIO_ACCESS_KEY: minioadmin
      MINIO_SECRET_KEY: minioadmin
    volumes:
      - milvus-minio:/minio_data

  standalone:
    image: milvusdb/milvus:v2.3.0
    command: ["./milvus", "run", "standalone"]
    environment:
      ETCD_ENDPOINTS: etcd:2379
      MINIO_ADDRESS: minio:9000
    ports:
      - "19530:19530"
    depends_on:
      - "etcd"
      - "minio"
```

---

### Week 2: 对话管理功能

#### Task 2.1: 实现会话管理器
**负责人**: 后端开发  
**优先级**: P0  
**预计时间**: 8小时  
**依赖**: Task 1.4

**子任务**:
- [ ] 设计会话数据结构
- [ ] 实现Redis缓存策略
- [ ] 实现会话生命周期管理
- [ ] 实现会话持久化
- [ ] 实现会话查询接口
- [ ] 实现会话清理机制
- [ ] 编写单元测试

**验收标准**:
- ✅ 可以创建和管理会话
- ✅ 会话状态正确维护
- ✅ 过期会话自动清理

**接口设计**:
```http
GET /api/ai/sessions
POST /api/ai/sessions
GET /api/ai/sessions/{sessionId}
DELETE /api/ai/sessions/{sessionId}
```

---

#### Task 2.2: 实现上下文管理器
**负责人**: 后端开发  
**优先级**: P0  
**预计时间**: 8小时  
**依赖**: Task 2.1

**子任务**:
- [ ] 设计上下文数据结构
- [ ] 实现对话历史存储
- [ ] 实现最近N轮对话管理
- [ ] 实现简单指代消解
- [ ] 实现上下文窗口管理
- [ ] 实现上下文缓存
- [ ] 编写单元测试

**验收标准**:
- ✅ 支持3-5轮对话上下文
- ✅ 指代消解基本可用
- ✅ 上下文缓存提升性能

---

#### Task 2.3: 实现意图识别器
**负责人**: 后端开发  
**优先级**: P1  
**预计时间**: 6小时  
**依赖**: Task 2.2

**子任务**:
- [ ] 设计意图分类体系
- [ ] 实现基于规则的识别
- [ ] 实现关键词匹配
- [ ] 实现意图路由
- [ ] 实现实体提取
- [ ] 编写测试用例
- [ ] 验证识别准确率

**验收标准**:
- ✅ 能识别基本意图类型
- ✅ 意图分类准确率 > 70%
- ✅ 实体提取基本可用

**意图类型**:
```java
public enum IntentType {
    GREETING,           // 问候
    PRODUCT_RECOMMENDATION,  // 商品推荐
    ORDER_QUERY,        // 订单查询
    PRICE_INQUIRY,      // 价格询问
    ATTRACTION_QUERY,   // 景点查询
    UNKNOWN
}
```

---

#### Task 2.4: 实现WebSocket通信
**负责人**: 后端开发  
**优先级**: P0  
**预计时间**: 8小时  
**依赖**: Task 1.4

**子任务**:
- [ ] 配置WebSocket支持
- [ ] 实现WebSocket端点
- [ ] 实现连接管理
- [ ] 实现消息收发
- [ ] 实现心跳检测
- [ ] 实现异常处理
- [ ] 编写集成测试

**验收标准**:
- ✅ WebSocket连接稳定
- ✅ 消息收发正常
- ✅ 心跳检测正常
- ✅ 异常情况处理完善

**WebSocket端点**:
```java
@ServerEndpoint(value = "/api/ai/chat/stream/{sessionId}")
public class ChatWebSocketEndpoint {
    @OnOpen
    public void onOpen(Session session, @PathParam("sessionId") String sessionId);
    
    @OnMessage
    public void onMessage(String message, @PathParam("sessionId") String sessionId);
    
    @OnClose
    public void onClose(Session session, @PathParam("sessionId") String sessionId);
    
    @OnError
    public void onError(Session session, Throwable error);
}
```

---

#### Task 2.5: 前端对话界面开发
**负责人**: 前端开发  
**优先级**: P0  
**预计时间**: 12小时  
**依赖**: Task 2.4

**子任务**:
- [ ] 设计对话界面组件
- [ ] 实现WebSocket客户端
- [ ] 实现消息展示
- [ ] 实现流式输出效果
- [ ] 实现用户输入框
- [ ] 实现历史记录查看
- [ ] 响应式设计优化
- [ ] 用户体验优化

**验收标准**:
- ✅ 界面美观易用
- ✅ WebSocket连接正常
- ✅ 流式输出流畅
- ✅ 响应式适配良好

**组件结构**:
```vue
<template>
  <div class="chat-container">
    <div class="chat-header">
      <h2>AI智能助手</h2>
      <el-button @click="viewHistory">历史记录</el-button>
    </div>
    <div class="chat-messages" ref="messagesContainer">
      <chat-message 
        v-for="message in messages" 
        :key="message.id"
        :message="message"
      />
    </div>
    <div class="chat-input">
      <el-input
        v-model="userInput"
        @keyup.enter="sendMessage"
        placeholder="输入您的问题..."
      />
      <el-button @click="sendMessage" type="primary">发送</el-button>
    </div>
  </div>
</template>
```

---

#### Task 2.6: 集成测试与修复
**负责人**: 全员  
**优先级**: P0  
**预计时间**: 8小时  
**依赖**: Task 2.1 - 2.5

**子任务**:
- [ ] 编写端到端测试用例
- [ ] 执行功能测试
- [ ] 执行性能测试
- [ ] 执行用户体验测试
- [ ] 收集测试问题
- [ ] 修复发现的问题
- [ ] 回归测试验证

**验收标准**:
- ✅ 所有测试用例通过
- ✅ 性能指标达标
- ✅ 无严重Bug
- ✅ 用户体验良好

---

## Phase 2: 知识库构建 (Week 3)

#### Task 3.1: 实现文档上传接口
**负责人**: 后端开发  
**优先级**: P0  
**预计时间**: 6小时  
**依赖**: Task 1.5

**子任务**:
- [ ] 设计文档数据模型
- [ ] 实现文件上传接口
- [ ] 实现文件类型校验
- [ ] 实现文件大小限制
- [ ] 实现文件存储 (MinIO/OSS)
- [ ] 实现上传进度反馈
- [ ] 编写接口文档

**验收标准**:
- ✅ 支持PDF、Word、TXT文档
- ✅ 文件大小限制 < 10MB
- ✅ 上传过程有进度提示

**API接口**:
```http
POST /api/ai/knowledge/upload
Content-Type: multipart/form-data

file: document.pdf
docType: attraction
location: 北京
category: 景点介绍

Response:
{
  "docId": "doc_123",
  "status": "processing",
  "message": "文档上传成功，正在向量化..."
}
```

---

#### Task 3.2: 实现文档向量化服务
**负责人**: 后端开发  
**优先级**: P0  
**预计时间**: 10小时  
**依赖**: Task 3.1

**子任务**:
- [ ] 实现文档预处理
- [ ] 实现文档切片算法
- [ ] 集成Embedding模型
- [ ] 实现批量向量化
- [ ] 实现向量存储到Milvus
- [ ] 实现向量化进度跟踪
- [ ] 编写单元测试

**验收标准**:
- ✅ 文档切片合理 (500字左右)
- [ ] 向量化成功率高
- [ ] 支持批量处理
- [ ] 进度跟踪准确

---

#### Task 3.3: 实现RAG检索功能
**负责人**: 后端开发  
**优先级**: P0  
**预计时间**: 8小时  
**依赖**: Task 3.2

**子任务**:
- [ ] 实现查询向量化
- [ ] 实现向量检索逻辑
- [ ] 实现相似度计算
- [ ] 实现结果排序
- [ ] 实现结果过滤
- [ ] 实现检索缓存
- [ ] 编写单元测试

**验收标准**:
- ✅ 检索响应时间 < 1秒
- ✅ 检索准确率 > 70%
- ✅ 支持多种过滤条件

---

#### Task 3.4: 实现知识库管理界面
**负责人**: 前端开发  
**优先级**: P1  
**预计时间**: 8小时  
**依赖**: Task 3.1

**子任务**:
- [ ] 设计文档列表页面
- [ ] 实现文档上传功能
- [ ] 实现文档删除功能
- [ ] 实现文档搜索功能
- [ ] 实现向量化状态显示
- [ ] 实现统计信息展示
- [ ] 用户体验优化

**验收标准**:
- ✅ 界面简洁易用
- ✅ 上传流程顺畅
- ✅ 状态显示清晰

---

#### Task 3.5: 初始化知识库数据
**负责人**: 后端开发  
**优先级**: P1  
**预计时间**: 6小时  
**依赖**: Task 3.2

**子任务**:
- [ ] 准备旅游文档样本
- [ ] 批量上传文档
- [ ] 执行向量化处理
- [ ] 验证检索效果
- [ ] 调整切片参数
- [ ] 优化检索质量
- [ ] 建立数据更新机制

**验收标准**:
- ✅ 至少50篇旅游文档
- ✅ 覆盖主要旅游场景
- ✅ 检索效果满足要求

---

## Phase 3: 业务集成 (Week 4)

#### Task 4.1: 实现商品推荐功能
**负责人**: 后端开发  
**优先级**: P0  
**预计时间**: 8小时  
**依赖**: Task 2.3

**子任务**:
- [ ] 设计推荐规则引擎
- [ ] 实现基于规则的推荐
- [ ] 集成商品服务 (Feign)
- [ ] 实现推荐结果格式化
- [ ] 实现推荐缓存
- [ ] 编写单元测试
- [ ] 验证推荐效果

**验收标准**:
- ✅ 推荐相关度 > 65%
- ✅ 推荐响应时间 < 2秒
- ✅ 支持多种推荐场景

---

#### Task 4.2: 实现订单查询功能
**负责人**: 后端开发  
**优先级**: P0  
**预计时间**: 6小时  
**依赖**: Task 2.3

**子任务**:
- [ ] 设计查询条件提取
- [ ] 实现自然语言解析
- [ ] 集成订单服务 (Feign)
- [ ] 实现查询结果格式化
- [ ] 实现自然语言描述生成
- [ ] 编写单元测试

**验收标准**:
- ✅ 支持常见查询场景
- ✅ 查询准确率 > 80%
- ✅ 结果描述清晰

---

#### Task 4.3: 集成用户服务
**负责人**: 后端开发  
**优先级**: P1  
**预计时间**: 4小时  
**依赖**: Task 4.1

**子任务**:
- [ ] 配置Feign Client
- [ ] 实现用户信息查询
- [ ] 实现用户偏好获取
- [ ] 实现用户历史行为获取
- [ ] 错误处理和降级

**验收标准**:
- ✅ 可以获取用户信息
- ✅ 错误处理完善
- ✅ 降级策略有效

---

#### Task 4.4: 前端功能完善
**负责人**: 前端开发  
**优先级**: P1  
**预计时间**: 10小时  
**依赖**: Task 2.5

**子任务**:
- [ ] 实现对话历史查看
- [ ] 实现用户设置功能
- [ ] 实现推荐结果展示
- [ ] 实现订单查询结果展示
- [ ] 实现收藏功能
- [ ] 实现反馈功能
- [ ] 响应式设计优化

**验收标准**:
- ✅ 功能完整
- ✅ 用户体验良好
- ✅ 响应式适配

---

#### Task 4.5: 业务测试与优化
**负责人**: 全员  
**优先级**: P0  
**预计时间**: 8小时  
**依赖**: Task 4.1 - 4.4

**子任务**:
- [ ] 推荐功能测试
- [ ] 查询功能测试
- [ ] 集成测试
- [ ] 性能测试
- [ ] 用户验收测试
- [ ] 问题修复
- [ ] 优化调整

**验收标准**:
- ✅ 所有业务功能正常
- ✅ 性能指标达标
- ✅ 用户反馈良好

---

## Phase 4: 完善测试 (Week 5)

#### Task 5.1: 性能优化
**负责人**: 后端开发  
**优先级**: P1  
**预计时间**: 8小时  
**依赖**: 所有开发任务

**子任务**:
- [ ] 分析性能瓶颈
- [ ] 优化缓存策略
- [ ] 优化数据库查询
- [ ] 优化API调用
- [ ] 实现异步处理
- [ ] 压力测试
- [ ] 性能调优

**验收标准**:
- ✅ 响应时间达标
- ✅ 并发能力达标
- ✅ 资源使用合理

---

#### Task 5.2: 功能完善
**负责人**: 全员  
**优先级**: P1  
**预计时间**: 8小时  
**依赖**: 所有开发任务

**子任务**:
- [ ] 错误处理完善
- [ ] 边界情况处理
- [ ] 用户体验优化
- [ ] 提示信息完善
- [ ] 帮助文档完善
- [ ] 用户引导优化

**验收标准**:
- ✅ 功能健壮性好
- ✅ 用户体验流畅
- ✅ 文档齐全

---

#### Task 5.3: 全面测试
**负责人**: 测试  
**优先级**: P0  
**预计时间**: 8小时  
**依赖**: Task 5.1, 5.2

**子任务**:
- [ ] 功能测试
- [ ] 性能测试
- [ ] 压力测试
- [ ] 安全测试
- [ ] 兼容性测试
- [ ] 用户验收测试
- [ ] 测试报告编写

**验收标准**:
- ✅ 所有测试通过
- ✅ 测试报告完整
- ✅ 无严重问题

---

#### Task 5.4: 文档完善
**负责人**: 全员  
**优先级**: P1  
**预计时间**: 6小时  
**依赖**: 所有开发任务

**子任务**:
- [ ] 编写API文档
- [ ] 编写部署文档
- [ ] 编写运维文档
- [ ] 编写用户手册
- [ ] 编写开发文档
- [ ] 文档审核

**验收标准**:
- ✅ 文档齐全
- ✅ 文档准确
- ✅ 文档易懂

---

#### Task 5.5: 部署上线
**负责人**: 运维  
**优先级**: P0  
**预计时间**: 8小时  
**依赖**: Task 5.3, 5.4

**子任务**:
- [ ] 生产环境配置
- [ ] 数据库初始化
- [ ] Milvus部署
- [ ] AI服务部署
- [ ] 前端部署
- [ ] 监控配置
- [ ] 灰度发布
- [ ] 上线验证

**验收标准**:
- ✅ 部署成功
- ✅ 服务正常运行
- ✅ 监控正常
- ✅ 用户可访问

---

## 📋 任务依赖关系

```
Task 1.1 → Task 1.2 → Task 1.3 → Task 1.4
                                     ↓
Task 1.5 ────────────────────────────→ Task 2.5
                                     ↓
Task 1.4 → Task 2.1 → Task 2.2 → Task 2.3 → Task 2.6
               ↓                              ↓
Task 1.5 → Task 3.1 → Task 3.2 → Task 3.3 → Task 3.5
               ↓              ↓              ↓
Task 2.3 → Task 4.1 → Task 4.2 → Task 4.3 → Task 4.5
                             ↓
All Tasks → Task 5.1 → Task 5.2 → Task 5.3 → Task 5.5
```

---

## ✅ 验收标准总结

### 功能验收
- [ ] 用户可以进行多轮对话 (3-5轮)
- [ ] 系统能基于知识库回答问题
- [ ] 支持商品推荐功能
- [ ] 支持订单查询功能
- [ ] 前端界面友好易用

### 性能验收
- [ ] 首次响应时间 < 3秒
- [ ] 流式输出延迟 < 500ms
- [ ] 并发支持 > 100用户
- [ ] 系统可用性 > 95%

### 质量验收
- [ ] 代码质量达标
- [ ] 测试覆盖率 > 60%
- [ ] 无严重Bug
- [ ] 文档完整

---

**任务清单状态**: 📝 已完成  
**总任务数**: 35  
**当前进度**: 0/35  
**预计开始时间**: 2026-04-17  
**预计完成时间**: 2026-05-21
