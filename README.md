# 智慧旅游助手平台

> 集成**秒杀、拼团、AI 助手、数字人直播**的一站式智慧旅游平台

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.0-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://www.oracle.com/java/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-blue.svg)](https://www.postgresql.org/)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://www.apache.org/licenses/LICENSE-2.0.html)

## 📋 项目简介

智慧旅游助手平台是一个基于 Spring Boot 3.2 的微服务架构项目，集成了高并发秒杀系统、拼团营销、AI 旅游规划助手和数字人直播带货功能。

### 核心功能

- ✅ **用户认证** - JWT 认证、手机号注册/登录
- ✅ **基础电商** - 景点门票、旅游商品、订单管理、支付对接
- ✅ **秒杀系统** - Redis Lua 原子扣库存、Kafka 削峰、AJ-Captcha 防刷
- ✅ **拼团系统** - 经典拼团（2-5人，24小时成团）
- ✅ **AI 助手** - GLM-5 旅游规划、智能推荐、RAG 向量检索
- ✅ **数字人直播** - 弹幕互动、AI 实时回复（MVP 10人并发）

## 🏗️ 技术架构

### 后端技术栈

| 技术 | 版本 | 用途 |
|------|------|------|
| Spring Boot | 3.2.0 | 核心框架 |
| Spring Security | 6.2.0 | 认证授权 |
| MyBatis-Plus | 3.5.5 | ORM 框架 |
| PostgreSQL | 15.0 | 主数据库 |
| Redis | 7.0 | 缓存 + 秒杀库存 |
| Redisson | 3.25.0 | 分布式锁 |
| Kafka | 3.6.0 | 消息队列 |
| RabbitMQ | 3.12.0 | 延迟队列 |
| Milvus | 2.3.0 | 向量数据库 |
| Sentinel | 1.8.6 | 限流熔断 |

### AI & 云服务

- **GLM-5 API** - 智谱AI 大模型
- **阿里云数字人** - 视频生成
- **阿里云 OSS** - 文件存储

## 🚀 快速开始

### 环境要求

- JDK 17+
- Maven 3.6+
- PostgreSQL 15+
- Redis 7.0+
- Kafka 3.6+
- RabbitMQ 3.12+

### 1. 克隆项目

```bash
git clone https://github.com/your-org/travel-assistant.git
cd travel-assistant
```

### 2. 初始化数据库

```bash
# 创建数据库并初始化表结构
psql -U postgres -d postgres -f database/init.sql
```

### 3. 修改配置

编辑 `travel-user/src/main/resources/application.yml`：

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/travel_assistant
    username: postgres
    password: your_password

  data:
    redis:
      host: localhost
      port: 6379
```

### 4. 启动服务

```bash
# 启动用户服务
cd travel-user
mvn spring-boot:run

# 访问文档
# http://localhost:8081/doc.html
```

### 5. 测试接口

```bash
# 用户注册
curl -X POST http://localhost:8081/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "phone": "13800138000",
    "smsCode": "123456",
    "password": "abc123"
  }'

# 用户登录
curl -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "phone": "13800138000",
    "password": "abc123"
  }'
```

## 📁 项目结构

```
travel-assistant/
├── travel-common/        # 公共模块
├── travel-user/          # 用户服务
├── travel-product/       # 商品服务
├── travel-order/         # 订单服务
├── travel-seckill/       # 秒杀服务
├── travel-group/         # 拼团服务
├── travel-ai/            # AI 服务
├── travel-live/          # 数字人直播
├── travel-gateway/       # 网关服务
├── database/             # 数据库脚本
└── docs/                 # 项目文档
```

## 🎯 开发路线图

### Phase 1: 基础框架 ✅
- [x] Spring Boot 多模块项目
- [x] 用户认证系统
- [x] 公共模块（Result、异常处理、JWT、Redis）

### Phase 2: 核心电商（开发中）
- [ ] 商品服务
- [ ] 订单服务
- [ ] 支付对接

### Phase 3: 秒杀系统（规划中）
- [ ] Redis 秒杀核心
- [ ] Kafka 削峰
- [ ] AJ-Captcha 防刷

### Phase 4: AI 能力（规划中）
- [ ] GLM-5 对话
- [ ] RAG 知识库
- [ ] 智能推荐

### Phase 5: 拼团 + 直播（规划中）
- [ ] 拼团系统
- [ ] 数字人直播

## 🔒 安全设计

- ✅ JWT Token 认证
- ✅ 密码 BCrypt 加密
- ✅ SQL 注入防护
- ✅ XSS 防护
- ✅ 接口限流（Sentinel）

## 📊 性能指标

| 接口类型 | QPS | 响应时间 | 并发 |
|---------|-----|---------|------|
| 首页/商品浏览 | 5000 | < 200ms (P99) | 2000 |
| 秒杀接口 | 10000 | < 100ms (P99) | 5000 |
| AI 对话 | 100 | < 2s | 50 |

## 📝 OpenSpec 变更管理

本项目使用 OpenSpec 管理变更提案和技术决策。

- 📄 [变更提案](./openspec/changes/smart-travel-assistant-mvp/proposal.md)
- 📐 [技术设计](./openspec/changes/smart-travel-assistant-mvp/design.md)
- 📋 [需求规格](./openspec/changes/smart-travel-assistant-mvp/specs/requirements.md)
- ✅ [任务清单](./openspec/changes/smart-travel-assistant-mvp/tasks.md)

## 🤝 贡献指南

欢迎贡献代码！请遵循以下步骤：

1. Fork 本仓库
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 提交 Pull Request

## 📄 许可证

本项目采用 Apache License 2.0 许可证 - 查看 [LICENSE](LICENSE) 文件了解详情。

## 🙏 致谢

- [Spring Boot](https://spring.io/projects/spring-boot)
- [MyBatis-Plus](https://baomidou.com/)
- [GLM-5](https://open.bigmodel.cn/)
- [Knife4j](https://doc.xiaominfo.com/)

## 📮 联系我们

- 项目地址：[https://github.com/your-org/travel-assistant](https://github.com/your-org/travel-assistant)
- 问题反馈：[Issues](https://github.com/your-org/travel-assistant/issues)
- 邮箱：dev@travel-assistant.com

---

⭐ 如果这个项目对你有帮助，请给我们一个 Star！
# Travel_Assistant-new
