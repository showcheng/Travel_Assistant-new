# Travel AI Service

智慧旅游助手 - AI服务模块

## 📋 功能概述

AI服务模块提供以下核心功能：

- ✅ GLM-5智能对话
- ✅ 多轮对话上下文管理
- ✅ 意图识别和路由
- ✅ 流式输出支持
- ✅ 会话历史管理
- ✅ 健康检查和监控

## 🚀 快速开始

### 1. 环境准备

**必需软件**:
- JDK 17+
- Maven 3.8+
- MySQL 8.0+ / PostgreSQL
- Redis 6.0+

**环境变量**:
```bash
# 设置GLM-5 API密钥
export GLM5_API_KEY=your-api-key-here

# 或在application.yml中配置
```

### 2. 数据库初始化

```bash
# 创建数据库
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS travel_assistant;"

# 初始化表结构
mysql -u root -p travel_assistant < src/main/resources/schema.sql
```

### 3. 启动服务

```bash
# 方式1: 使用Maven
cd travel-assistant/travel-ai
mvn spring-boot:run

# 方式2: 使用启动脚本
./start-ai.sh

# 方式3: 打包后运行
mvn clean package
java -jar target/travel-ai-1.0.0.jar
```

### 4. 验证服务

```bash
# 健康检查
curl http://localhost:8086/api/ai/health

# 运行API测试
./test-ai-api.sh
```

## 📡 API接口

### 1. 发送消息

```http
POST /api/ai/chat/send
Content-Type: application/json

{
  "message": "你好，请介绍一下自己",
  "sessionId": "optional-session-id",
  "stream": false
}
```

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "sessionId": "session-123",
    "response": "您好！我是智慧旅游助手...",
    "intentType": "GREETING",
    "tokens": 150,
    "timestamp": "2026-04-16T15:30:00",
    "finished": true
  }
}
```

### 2. 获取会话历史

```http
GET /api/ai/chat/history/{sessionId}?limit=10
```

### 3. 清除会话历史

```http
DELETE /api/ai/chat/history/{sessionId}
```

### 4. 健康检查

```http
GET /api/ai/health
```

## 🔧 配置说明

### application.yml

```yaml
server:
  port: 8086

spring:
  application:
    name: travel-ai

# GLM-5 API配置
glm5:
  api:
    key: ${GLM5_API_KEY}
    url: https://open.bigmodel.cn/api/paas/v4/
    model: glm-4-flash
    temperature: 0.7
    max-tokens: 2000
    timeout: 30000
```

### 获取GLM-5 API密钥

1. 访问 [智谱AI开放平台](https://open.bigmodel.cn/)
2. 注册并登录
3. 进入API Keys页面
4. 创建新的API密钥
5. 将密钥设置为环境变量

## 📦 项目结构

```
travel-ai/
├── src/main/java/com/travel/ai/
│   ├── config/           # 配置类
│   │   └── GLM5Config.java
│   ├── controller/       # 控制器
│   │   ├── AIChatController.java
│   │   └── AIHealthController.java
│   ├── service/          # 服务层
│   │   ├── AIService.java
│   │   └── impl/
│   │       └── AIServiceImpl.java
│   ├── entity/           # 实体类
│   │   ├── ConversationSession.java
│   │   └── ConversationMessage.java
│   ├── dto/              # 数据传输对象
│   │   ├── ChatRequest.java
│   │   └── ChatResponse.java
│   └── mapper/           # 数据访问层
│       ├── ConversationSessionMapper.java
│       └── ConversationMessageMapper.java
├── src/main/resources/
│   ├── application.yml
│   └── schema.sql
├── start-ai.sh          # 启动脚本
└── pom.xml
```

## 🧪 测试

### 单元测试
```bash
mvn test
```

### API测试
```bash
./test-ai-api.sh
```

### 手动测试
```bash
# 简单对话
curl -X POST http://localhost:8086/api/ai/chat/send \
  -H "Content-Type: application/json" \
  -d '{"message": "你好"}'
```

## 📊 监控和日志

### 日志位置
- 控制台输出: 实时日志
- 文件日志: `logs/travel-ai.log`

### 日志级别配置
```yaml
logging:
  level:
    com.travel.ai: DEBUG
    dev.langchain4j: INFO
```

## 🚨 故障排查

### 问题1: GLM-5 API调用失败

**症状**: 返回"AI服务暂时不可用"

**解决方案**:
1. 检查API密钥是否正确设置
2. 检查网络连接是否正常
3. 查看日志中的详细错误信息

### 问题2: 服务启动失败

**症状**: 启动时报错

**解决方案**:
1. 检查端口8086是否被占用
2. 检查数据库连接是否正常
3. 检查Redis连接是否正常

### 问题3: 编译失败

**症状**: mvn compile失败

**解决方案**:
```bash
# 清理并重新编译
mvn clean compile -U

# 跳过测试
mvn clean package -DskipTests
```

## 📝 开发计划

- [x] Task 1.1: 创建AI服务模块框架 ✅
- [x] Task 1.2: 集成LangChain4j框架 ✅
- [ ] Task 1.3: 配置GLM-5 API连接
- [ ] Task 1.4: 实现简单对话API
- [ ] Task 1.5: 部署Milvus向量数据库

## 📚 相关文档

- [OpenSpec任务计划](../../openspec/changes/ai-intelligent-assistant-mvp/tasks.md)
- [技术设计文档](../../openspec/changes/ai-intelligent-assistant-mvp/design.md)
- [API接口文档](../../openspec/changes/ai-intelligent-assistant-mvp/specs/core-dialogue/spec.md)

## 🤝 贡献指南

欢迎提交Issue和Pull Request！

## 📄 许可证

MIT License
