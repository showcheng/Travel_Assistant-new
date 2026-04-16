# 需求规格说明：智慧旅游助手 MVP

## 用户故事

### US-001: 用户注册与登录
**作为** 新用户
**我想要** 注册账号并登录系统
**以便** 购买旅游产品和享受服务

**验收标准：**
- [ ] 支持手机号 + 验证码注册
- [ ] 支持密码登录（6-20 位，包含字母和数字）
- [ ] 登录成功后返回 JWT Token
- [ ] Token 有效期 7 天
- [ ] 支持忘记密码功能

---

### US-002: 浏览景点门票
**作为** 游客
**我想要** 浏览热门景点门票
**以便** 选择合适的旅游产品

**验收标准：**
- [ ] 展示景点列表（图片、名称、价格、库存）
- [ ] 支持按价格、热度筛选
- [ ] 支持关键词搜索
- [ ] 点击查看详情（使用说明、退改政策）

---

### US-003: 购买景点门票
**作为** 用户
**我想要** 购买景点门票
**以便** 预订我的行程

**验收标准：**
- [ ] 选择日期、数量
- [ ] 实时显示库存
- [ ] 选择游玩人信息
- [ ] 确认订单（总价、优惠）
- [ ] 跳转支付（30 分钟内完成）
- [ ] 支付成功生成电子票（二维码）

---

### US-004: 秒杀抢购
**作为** 用户
**我想要** 参与秒杀活动
**以便** 低价抢购热门产品

**验收标准：**
- [ ] 秒杀页面显示倒计时
- [ ] 显示实时库存（1000 份）
- [ ] 秒杀开始前按钮置灰
- [ ] 开始后点击"立即抢购"
- [ ] 通过滑块验证码验证
- [ ] 抢购成功跳转支付（5 分钟内完成）
- [ ] 支付超时自动取消，库存释放

**性能要求：**
- 支持 5000 人同时抢购
- 抢购响应时间 < 1 秒
- 0 超卖

---

### US-005: 拼团购买
**作为** 用户
**我想要** 发起/参与拼团
**以便** 享受团购优惠

**验收标准：**
- [ ] 选择拼团商品（2-5 人成团）
- [ ] 发起拼团（团长先支付）
- [ ] 分享拼团链接
- [ ] 查看拼团进度（已参团人数）
- [ ] 24 小时内成团
- [ ] 成团后统一发货/出票
- [ ] 未成团自动退款

---

### US-006: AI 旅游规划
**作为** 用户
**我想要** AI 帮我规划行程
**以便** 节省做攻略的时间

**验收标准：**
- [ ] 输入：目的地、天数、预算、偏好
- [ ] AI 生成每日行程（上午/下午/晚上）
- [ ] 推荐景点 + 交通方式
- [ ] 推荐相关商品（门票、特产）
- [ ] 支持调整行程（拖拽、删除）
- [ ] 一键购买行程中的产品

**AI 能力要求：**
- 行程合理性（距离、时间）
- 个性化推荐（根据历史行为）
- 推荐准确率 > 85%

---

### US-007: AI 商品推荐
**作为** 用户
**我想要** 系统推荐我可能喜欢的商品
**以便** 发现更多感兴趣的产品

**验收标准：**
- [ ] 首页个性化推荐（协同过滤）
- [ ] 商品详情页"相似推荐"
- [ ] 购物车"智能搭配"
- [ ] 基于搜索历史推荐

---

### US-008: 数字人直播互动
**作为** 用户
**我想要** 观看数字人直播
**以便** 了解产品并互动提问

**验收标准：**
- [ ] 观看直播视频流
- [ ] 发送弹幕提问
- [ ] 数字人实时回答（< 10 秒响应）
- [ ] 点击商品卡片购买
- [ ] 显示商品库存、价格

**性能要求（MVP）：**
- 支持 10 人同时互动
- 弹幕响应时间 < 10 秒

---

### US-009: 订单管理
**作为** 用户
**我想要** 查看我的订单
**以便** 了解订单状态

**验收标准：**
- [ ] 订单列表（全部/待支付/已支付/已完成）
- [ ] 订单详情（商品、价格、状态）
- [ ] 待支付订单继续支付
- [ ] 申请退款
- [ ] 查看退款进度

---

### US-010: 支付功能
**作为** 用户
**我想要** 安全便捷地支付
**以便** 完成购买

**验收标准：**
- [ ] 支持支付宝
- [ ] 支持微信支付
- [ ] 支付成功自动跳转
- [ ] 支付失败重试
- [ ] 30 分钟内未支付自动取消

---

## 非功能性需求

### 性能要求

| 接口类型 | QPS | 响应时间 | 并发 |
|---------|-----|---------|------|
| 首页/商品浏览 | 5000 | < 200ms (P99) | 2000 |
| 秒杀接口 | 10000 | < 100ms (P99) | 5000 |
| AI 对话 | 100 | < 2s | 50 |
| 订单创建 | 2000 | < 300ms | 1000 |

### 安全要求

- [ ] 所有 API 使用 JWT 认证
- [ ] 密码使用 BCrypt 加密存储
- [ ] 支付回调验签
- [ ] SQL 注入防护（参数化查询）
- [ ] XSS 防护（输入过滤）
- [ ] 接口限流（单用户 10 次/秒）

### 可用性要求

- [ ] 系统可用性 > 99.5%
- [ ] 数据库主从备份
- [ ] Redis Sentinel 高可用
- [ ] 核心接口降级策略

### 可扩展性要求

- [ ] 支持水平扩展（无状态服务）
- [ ] 消息队列解耦
- [ ] 缓存分层设计

---

## 数据模型

### 核心实体

```sql
-- 用户表
CREATE TABLE user (
    id BIGINT PRIMARY KEY,
    phone VARCHAR(20) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    nickname VARCHAR(50),
    avatar_url VARCHAR(255),
    created_at DATETIME,
    updated_at DATETIME
);

-- 商品表
CREATE TABLE product (
    id BIGINT PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    type ENUM('TICKET', 'PRODUCT'),
    price DECIMAL(10, 2) NOT NULL,
    original_price DECIMAL(10, 2),
    stock INT DEFAULT 0,
    sales INT DEFAULT 0,
    cover_image VARCHAR(255),
    detail TEXT,
    created_at DATETIME
);

-- 秒杀商品表
CREATE TABLE seckill_sku (
    id BIGINT PRIMARY KEY,
    product_id BIGINT,
    seckill_price DECIMAL(10, 2) NOT NULL,
    stock INT DEFAULT 1000,
    start_time DATETIME NOT NULL,
    end_time DATETIME NOT NULL,
    INDEX idx_time (start_time, end_time)
);

-- 订单表
CREATE TABLE \`order\` (
    id BIGINT PRIMARY KEY,
    order_no VARCHAR(64) UNIQUE NOT NULL,
    user_id BIGINT NOT NULL,
    total_amount DECIMAL(10, 2) NOT NULL,
    status ENUM('PENDING', 'PAID', 'CANCELLED', 'REFUNDED'),
    created_at DATETIME,
    pay_time DATETIME,
    INDEX idx_user (user_id),
    INDEX idx_status (status)
);

-- 拼团表
CREATE TABLE group_buy (
    id BIGINT PRIMARY KEY,
    product_id BIGINT,
    leader_user_id BIGINT,
    target_size INT NOT NULL,
    current_size INT DEFAULT 1,
    status ENUM('PENDING', 'COMPLETED', 'CANCELLED'),
    expire_time DATETIME NOT NULL,
    created_at DATETIME
);

-- AI 对话历史表
CREATE TABLE chat_history (
    id BIGINT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    message TEXT NOT NULL,
    response TEXT,
    created_at DATETIME,
    INDEX idx_user_time (user_id, created_at)
);
```

---

## API 接口定义

### 用户模块

```
POST /api/auth/register
POST /api/auth/login
POST /api/auth/send-sms
POST /api/auth/reset-password
```

### 商品模块

```
GET /api/products
GET /api/products/{id}
GET /api/products/search
```

### 秒杀模块

```
GET /api/seckill/list
POST /api/seckill/{skuId}/order
GET /api/seckill/result/{orderId}
```

### 拼团模块

```
POST /api/group-buy/create
POST /api/group-buy/{groupId}/join
GET /api/group-buy/my-groups
```

### AI 模块

```
POST /api/ai/chat
POST /api/ai/plan-trip
POST /api/ai/recommend
```

### 订单模块

```
POST /api/orders/create
GET /api/orders/list
GET /api/orders/{id}
POST /api/orders/{id}/pay
POST /api/orders/{id}/cancel
POST /api/orders/{id}/refund
```

---

## 测试场景

### 秒杀场景

```
场景：5000 人同时抢购 1000 份商品
────────────────────────────────────────
1. 提前预热：库存加载到 Redis
2. 倒计时结束，5000 人同时点击"立即抢购"
3. 通过滑块验证码（过滤 20% 机器人）
4. Redis Lua 原子扣减库存
5. 扣减成功的 4000 人发送 Kafka 消息
6. 消费者异步创建订单（限流 200 QPS）
7. 前 1000 人创建成功，后 3000 人提示"已抢完"

预期结果：
✅ 库存准确（1000 份）
✅ 无超卖
✅ 无数据丢失
✅ 响应时间 < 1s
```

### 拼团场景

```
场景：用户发起 3 人拼团
────────────────────────────────────
1. 用户 A 发起拼团（团长，已支付）
2. 分享链接给用户 B、C
3. 用户 B 参团（支付）
4. 用户 C 参团（支付）
5. 拼团成功，触发发货

异常情况：
- 24 小时未成团 → 自动退款
- 团员取消订单 → 拼团失败
```

---

## 验收标准总结

### 功能完整性
- [ ] 所有用户故事实现
- [ ] 所有 API 接口可用
- [ ] 支付流程打通

### 性能达标
- [ ] 秒杀系统压测通过（5000 并发）
- [ ] 接口响应时间达标
- [ ] 无内存泄漏

### 安全合规
- [ ] 安全扫描通过
- [ ] 渗透测试通过
- [ ] 支付认证通过

### 用户体验
- [ ] 核心流程可用性测试通过
- [ ] 用户反馈收集
- [ ] Bug 修复
