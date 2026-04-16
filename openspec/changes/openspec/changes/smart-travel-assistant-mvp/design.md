# 技术设计文档：智慧旅游助手 MVP

## 系统架构

### 整体架构图

系统采用分层微服务架构，包含前端网关、业务服务层、中间件层和数据层。

主要服务模块：
- 用户服务（认证、用户信息）
- 商品服务（商品管理、库存）
- 订单服务（订单创建、支付）
- 秒杀服务（高并发秒杀）
- 拼团服务（拼团管理）
- AI 服务（GLM-5 对话、推荐）
- 数字人直播服务（弹幕互动）

---

## 秒杀系统详细设计

### 核心技术决策

| 决策点 | 方案 | 理由 |
|--------|------|------|
| 库存扣减 | Redis Lua + 定时对账 | 性能优先 |
| 库存回滚 | 批量延迟回滚（5分钟） | 平衡性能与一致性 |
| 防刷验证 | AJ-Captcha 滑块验证 | 开源免费 |
| 削峰方案 | Kafka + 限流消费 | 解耦订单创建 |

### Redis Lua 核心脚本

```lua
-- 秒杀扣库存 Lua 脚本
local stockKey = KEYS[1]
local userKey = KEYS[2]
local orderId = ARGV[1]
local userId = ARGV[2]

-- 检查是否重复抢购
if redis.call('EXISTS', userKey) == 1 then
    return -1
end

-- 检查库存
local stock = tonumber(redis.call('GET', stockKey))
if stock == nil or stock <= 0 then
    return 0
end

-- 扣减库存
redis.call('DECRBY', stockKey, 1)
redis.call('SET', userKey, orderId)
redis.call('EXPIRE', userKey, 3600)

return 1
```

### 秒杀数据表设计

```sql
CREATE TABLE seckill_sku (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    product_id BIGINT NOT NULL,
    sku_name VARCHAR(200) NOT NULL,
    original_price DECIMAL(10,2) NOT NULL,
    seckill_price DECIMAL(10,2) NOT NULL,
    stock_count INT NOT NULL DEFAULT 1000,
    start_time DATETIME NOT NULL,
    end_time DATETIME NOT NULL,
    status TINYINT DEFAULT 1,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_time (start_time, end_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

---

## AI 服务设计

### RAG 系统架构

1. 向量化：用户问题 → Embedding 向量
2. 向量检索：查询 Milvus 获取相关景点、商品
3. 构建 Prompt：检索结果 + 用户问题
4. GLM-5 生成：返回 JSON 格式行程 + 商品推荐

---

## 数字人直播设计（MVP）

### 简化架构

- WebSocket 接收弹幕（10人并发）
- 意图识别 + 合并相似问题
- GLM-5 生成回复
- 调用云端 API 生成视频
- WebSocket 推送视频 URL

---

## 技术栈清单

### 后端技术栈

| 技术 | 版本 | 用途 |
|------|------|------|
| Spring Boot | 3.2.0 | 核心框架 |
| Spring Security | 6.2.0 | 认证授权 |
| MyBatis-Plus | 3.5.5 | ORM 框架 |
| Redis | 7.0 | 缓存 + 秒杀库存 |
| Kafka | 3.6.0 | 消息队列 |
| RabbitMQ | 3.12.0 | 延迟队列 |
| PostgreSQL | 15.0 | 主数据库 |
| Milvus | 2.3.0 | 向量数据库 |

### 前端技术栈

| 技术 | 版本 | 用途 |
|------|------|------|
| Vue.js | 3.3.0 | 前端框架 |
| Vite | 5.0.0 | 构建工具 |
| Pinia | 2.1.0 | 状态管理 |
| Element Plus | 2.4.0 | UI 组件库 |

---

## 性能优化策略

### 秒杀性能优化

| 优化点 | 方案 | 预期效果 |
|--------|------|---------|
| Redis 预热 | 提前加载库存到 Redis | 减少数据库压力 |
| 本地缓存 | Guava Cache 缓存商品信息 | 减少 Redis 查询 |
| 批量处理 | Kafka 批量消费 | 提升吞吐量 5 倍 |
| 异步处理 | 订单创建异步化 | 响应时间减少 80% |

---

## 监控与告警

### 监控指标

- 应用层：QPS、响应时间、错误率、JVM 内存
- 中间件：Redis 命中率、Kafka 消息堆积
- 业务：秒杀成功率、订单转化率、AI 准确率

---

## 安全设计

### 安全防护清单

- JWT Token 认证
- API 接口签名验证
- SQL 注入防护
- XSS 防护
- CSRF 防护
- 接口限流（Sentinel）
- 敏感数据加密
- 访问日志审计
