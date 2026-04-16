# 核心对话功能规格

## 📋 功能概述

提供智能对话核心能力，支持多轮对话、上下文理解、意图识别和自然语言交互。

## 🎯 功能需求

### 1. 会话管理

#### 1.1 会话生命周期

**功能描述**: 管理对话会话的完整生命周期

**用户故事**:
```
作为用户
我想要有一个持久的对话会话
这样我可以随时继续之前的对话
```

**功能点**:
- ✅ 创建新会话
- ✅ 恢复历史会话
- ✅ 会话状态管理 (活跃/空闲/关闭)
- ✅ 会话自动过期 (24小时无活动)
- ✅ 会话删除

**API接口**:
```http
POST /api/ai/sessions
GET /api/ai/sessions
GET /api/ai/sessions/{sessionId}
DELETE /api/ai/sessions/{sessionId}
GET /api/ai/sessions/{sessionId}/messages
```

**数据结构**:
```json
{
  "sessionId": "session_123",
  "userId": 1,
  "title": "关于北京旅游的咨询",
  "state": "active",
  "messageCount": 5,
  "createdAt": "2026-04-16T14:30:00",
  "lastActiveTime": "2026-04-16T15:45:00"
}
```

**验收标准**:
- [ ] 会话创建成功率 > 99%
- [ ] 会话恢复时间 < 500ms
- [ ] 会话状态正确维护
- [ ] 过期会话自动清理

---

#### 1.2 多轮对话上下文

**功能描述**: 维护对话上下文，支持指代消解和多轮理解

**用户故事**:
```
作为用户
我想要系统记住我之前说的话
这样我就不用重复描述
```

**功能点**:
- ✅ 保存最近3-5轮对话
- ✅ 简单的指代消解 ("它"、"这个")
- ✅ 上下文信息继承
- ✅ 对话历史查询

**对话示例**:
```
用户: 北京有什么好玩的？
AI:  北京有很多著名景点，比如故宫、长城、天坛等。

用户: 它门票多少钱？
AI:  故宫成人票60元，学生票20元... (理解"它"指故宫)

用户: 长城呢？
AI:  长城一日游188元，八达岭长城门票40元... (理解上下文)
```

**验收标准**:
- [ ] 支持至少3轮对话
- [ ] 指代消解准确率 > 60%
- [ ] 上下文信息正确传递

---

### 2. 意图识别

#### 2.1 意图分类

**功能描述**: 识别用户对话的真实意图

**支持的意图类型**:
```java
public enum IntentType {
    // 对话意图
    GREETING,              // 问候: "你好", "您好"
    FAREWELL,              // 告别: "再见", "拜拜"
    COMPLIMENT,            // 赞美: "很好", "太棒了"
    COMPLAINT,             // 投诉: "不好", "有问题"
    
    // 业务意图
    PRODUCT_RECOMMENDATION, // 商品推荐: "推荐个景点"
    ORDER_QUERY,           // 订单查询: "我的订单"
    PRICE_INQUIRY,         // 价格询问: "多少钱"
    
    // 旅游意图
    ATTRACTION_QUERY,      // 景点查询: "有什么好玩的"
    ROUTE_PLANNING,        // 路线规划: "怎么安排行程"
    WEATHER_QUERY,         // 天气查询: "明天天气怎么样"
    
    // 未知意图
    UNKNOWN
}
```

**API接口**:
```http
POST /api/ai/intent/recognize
Content-Type: application/json

{
  "message": "北京有什么好玩的景点？",
  "sessionId": "session_123",
  "context": {...}
}

Response:
{
  "intentType": "ATTRACTION_QUERY",
  "confidence": 0.85,
  "entities": {
    "location": "北京",
    "category": "景点"
  }
}
```

**验收标准**:
- [ ] 意图识别准确率 > 70%
- [ ] 识别响应时间 < 200ms
- [ ] 支持至少8种意图类型

---

#### 2.2 实体提取

**功能描述**: 从用户消息中提取关键实体信息

**支持的实体类型**:
```java
- Location (地点): "北京", "上海", "故宫"
- TimeRange (时间): "明天", "本周", "最近"
- Price (价格): "100元以下", "性价比高"
- Category (类别): "景点", "酒店", "美食"
- Preference (偏好): "适合老人", "家庭出游"
```

**提取示例**:
```
输入: "推荐几个适合老人的北京景点"
输出: {
  "location": "北京",
  "target_audience": "老人",
  "category": "景点"
}
```

**验收标准**:
- [ ] 实体提取准确率 > 65%
- [ ] 支持至少5种实体类型
- [ ] 提取响应时间 < 100ms

---

### 3. 实时通信

#### 3.1 WebSocket连接

**功能描述**: 提供WebSocket实时双向通信

**连接端点**:
```
ws://localhost:8080/api/ai/chat/stream/{sessionId}
```

**心跳机制**:
- 客户端每30秒发送ping
- 服务端返回pong
- 超过60秒无心跳自动断开

**重连机制**:
- 断线自动重连 (最多3次)
- 重连间隔: 1s, 2s, 5s
- 重连后恢复会话上下文

**验收标准**:
- [ ] WebSocket连接稳定
- [ ] 支持并发连接 > 100
- [ ] 心跳检测正常
- [ ] 重连机制有效

---

#### 3.2 流式输出

**功能描述**: 实现AI回复的流式输出，提升用户体验

**输出格式**:
```json
{
  "type": "token",
  "content": "北",
  "sessionId": "session_123",
  "sequence": 1
}

{
  "type": "token",
  "content": "京",
  "sessionId": "session_123",
  "sequence": 2
}

{
  "type": "done",
  "sessionId": "session_123",
  "totalTokens": 156
}
```

**验收标准**:
- [ ] 流式输出延迟 < 500ms
- [ ] 输出过程流畅
- [ ] 支持长文本输出
- [ ] 错误处理完善

---

### 4. 错误处理

#### 4.1 异常情况处理

**功能描述**: 优雅处理各种异常情况

**处理的异常类型**:
- ✅ API调用失败 (降级处理)
- ✅ 超时处理 (3秒超时)
- ✅ 无效输入 (友好提示)
- ✅ 会话不存在 (自动创建)
- ✅ 并发冲突 (队列处理)

**错误响应格式**:
```json
{
  "error": true,
  "errorCode": "AI_SERVICE_TIMEOUT",
  "message": "AI服务暂时不可用，请稍后重试",
  "retryAfter": 5
}
```

**验收标准**:
- [ ] 所有异常都有处理
- [ ] 错误提示友好
- [ ] 无系统崩溃
- [ ] 错误日志完整

---

## 📊 性能要求

### 响应时间
- 首次响应: < 3秒
- 流式输出延迟: < 500ms
- 意图识别: < 200ms
- 会话恢复: < 500ms

### 并发能力
- 同时在线用户: > 100
- 每秒请求数: > 50 QPS
- WebSocket连接: > 100

### 可用性
- 系统可用性: > 95%
- 故障恢复时间: < 5分钟
- 数据持久化: > 99%

---

## 🧪 测试要求

### 功能测试
- [ ] 会话创建和管理
- [ ] 多轮对话上下文
- [ ] 意图识别准确性
- [ ] 实体提取准确性
- [ ] WebSocket通信稳定性
- [ ] 流式输出效果

### 性能测试
- [ ] 响应时间测试
- [ ] 并发压力测试
- [ ] 长连接稳定性测试
- [ ] 内存泄漏测试

### 用户体验测试
- [ ] 对话流畅度
- [ ] 界面友好度
- [ ] 错误提示清晰度
- [ ] 整体满意度

---

## 📚 API文档

### 核心接口

#### 1. 发送消息
```http
POST /api/ai/chat/send
Content-Type: application/json

{
  "message": "北京有什么好玩的？",
  "sessionId": "session_123"  // 可选，不提供则创建新会话
}

Response 200:
{
  "sessionId": "session_123",
  "response": "北京有很多著名景点...",
  "intent": {
    "type": "ATTRACTION_QUERY",
    "confidence": 0.85
  },
  "timestamp": "2026-04-16T14:30:00"
}
```

#### 2. 获取会话列表
```http
GET /api/ai/sessions
Query Params:
  - page: 0
  - size: 10
  - status: active (可选)

Response 200:
{
  "total": 5,
  "sessions": [
    {
      "sessionId": "session_123",
      "title": "关于北京旅游的咨询",
      "state": "active",
      "messageCount": 5,
      "lastActiveTime": "2026-04-16T15:45:00"
    }
  ]
}
```

#### 3. 获取会话详情
```http
GET /api/ai/sessions/{sessionId}/messages
Query Params:
  - limit: 10 (最近N条消息)

Response 200:
{
  "sessionId": "session_123",
  "messages": [
    {
      "id": "msg_1",
      "role": "USER",
      "content": "北京有什么好玩的？",
      "timestamp": "2026-04-16T14:30:00"
    },
    {
      "id": "msg_2",
      "role": "ASSISTANT",
      "content": "北京有很多著名景点...",
      "timestamp": "2026-04-16T14:30:02"
    }
  ]
}
```

---

## 🎯 验收标准

### 功能完整性
- [ ] 所有核心功能实现
- [ ] API接口完整
- [ ] 错误处理完善
- [ ] 用户体验良好

### 性能达标
- [ ] 响应时间符合要求
- [ ] 并发能力满足需求
- [ ] 系统稳定性良好

### 质量标准
- [ ] 代码质量达标
- [ ] 测试覆盖率 > 60%
- [ ] 文档完整准确
- [ ] 无严重Bug

---

**规格状态**: ✅ 已完成  
**版本**: 1.0.0  
**最后更新**: 2026-04-16
