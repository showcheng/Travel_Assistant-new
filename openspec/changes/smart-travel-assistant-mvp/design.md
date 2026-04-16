# 技术设计文档：智慧旅游助手 MVP

## 系统架构

### 整体架构图

```
┌─────────────────────────────────────────────────────────────────┐
│                        智慧旅游助手系统架构                        │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌──────────────┐    ┌──────────────┐    ┌──────────────┐      │
│  │  Web 前端    │    │  管理后台    │    │  移动端      │      │
│  │  (Vue.js)    │    │  (Vue.js)    │    │  (H5)        │      │
│  └──────────────┘    └──────────────┘    └──────────────┘      │
│         │                    │                    │              │
│         └────────────────────┼────────────────────┘              │
│                              ▼                                   │
│  ┌─────────────────────────────────────────────────────────────┐│
│  │                   API 网关 (Nginx + Spring Cloud Gateway)   ││
│  │  • 限流、熔断、负载均衡                                      ││
│  │  • JWT 验证                                                 ││
│  └─────────────────────────────────────────────────────────────┘│
│                              │                                   │
│         ┌────────────────────┼────────────────────┐              │
│         ▼                    ▼                    ▼              │
│  ┌──────────────┐    ┌──────────────┐    ┌──────────────┐      │
│  │  用户服务    │    │  商品服务    │    │  订单服务    │      │
│  │  • 注册登录  │    │  • 商品管理  │    │  • 订单创建  │      │
│  │  • 用户信息  │    │  • 库存管理  │    │  • 支付对接  │      │
│  └──────────────┘    └──────────────┘    └──────────────┘      │
│                                                                 │
│  ┌──────────────┐    ┌──────────────┐    ┌──────────────┐      │
│  │  秒杀服务    │    │  拼团服务    │    │  AI 服务      │      │
│  │  • Redis扣减 │    │  • 拼团管理  │    │  • GLM-5对话 │      │
│  │  • 防刷验证  │    │  • 状态机    │    │  • 智能推荐  │      │
│  └──────────────┘    └──────────────┘    └──────────────┘      │
│                                                                 │
│  ┌─────────────────────────────────────────────────────────────┐│
│  │                   数字人直播服务                            ││
│  │  • 弹幕处理  • GLM-5 生成  • 云端数字人 API                 ││
│  └─────────────────────────────────────────────────────────────┘│
│                                                                 │
├─────────────────────────────────────────────────────────────────┤
│                          中间件层                               │
│  ┌──────────────┐    ┌──────────────┐    ┌──────────────┐      │
│  │  Redis       │    │  Kafka       │    │  RabbitMQ    │      │
│  │  • 缓存      │    │  • 削峰      │    │  • 延迟队列  │      │
│  │  • 秒杀库存  │    │  • 异步解耦  │    │  • 订单超时  │      │
│  └──────────────┘    └──────────────┘    └──────────────┘      │
│                                                                 │
├─────────────────────────────────────────────────────────────────┤
│                          数据层                                 │
│  ┌──────────────┐    ┌──────────────┐    ┌──────────────┐      │
│  │  PostgreSQL  │    │  Milvus      │    │  OSS/MinIO   │      │
│  │  • 业务数据  │    │  • 向量检索  │    │  • 文件存储  │      │
│  │  • 订单数据  │    │  • RAG 知识库│    │  • 图片视频  │      │
│  └──────────────┘    └──────────────┘    └──────────────┘      │
│                                                                 │
├─────────────────────────────────────────────────────────────────┤
│                          外部服务                               │
│  ┌──────────────┐    ┌──────────────┐    ┌──────────────┐      │
│  │  支付宝/微信 │    │  短信服务    │    │  GLM-5 API   │      │
│  │  • 支付      │    │  • 验证码    │    │  • AI 对话   │      │
│  └──────────────┘    └──────────────┘    └──────────────┘      │
│  ┌──────────────┐    ┌──────────────┐                          │
│  │  阿里云数字人│    │  对象存储    │                          │
│  │  • 视频生成  │    │  • CDN       │                          │
│  └──────────────┘    └──────────────┘                          │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

---

## 秒杀系统详细设计

### 核心技术决策

基于深入讨论，采用以下方案：

| 决策点 | 方案 | 理由 |
|--------|------|------|
| **库存扣减** | Redis Lua + 定时对账 | 性能优先，Redis 高可用兜底 |
| **库存回滚** | 批量延迟回滚（5分钟） | 平衡性能与一致性 |
| **防刷验证** | AJ-Captcha 滑块验证 | 开源免费，效果好 |
| **削峰方案** | Kafka + 限流消费 | 解耦订单创建 |

### 秒杀流程时序图

```
┌─────────┐    ┌─────────┐    ┌─────────┐    ┌─────────┐    ┌─────────┐
│  用户   │    │  网关   │    │ 秒杀服务 │   │  Redis  │    │  Kafka  │
└────┬────┘    └────┬────┘    └────┬────┘    └────┬────┘    └────┬────┘
     │              │              │              │              │
     │ 1. 点击秒杀   │              │              │              │
     │─────────────▶│              │              │              │
     │              │              │              │              │
     │              │ 2. 限流检查  │              │              │
     │              │─────────────▶│              │              │
     │              │              │              │              │
     │              │ 3. 滑块验证  │              │              │
     │              │◀────────────│              │              │
     │ 4. 完成验证   │              │              │              │
     │─────────────▶│              │              │              │
     │              │              │              │              │
     │              │              │ 5. Lua扣库存 │              │
     │              │              │─────────────▶│              │
     │              │              │              │              │
     │              │              │ 6. 返回结果  │              │
     │              │              │◀────────────│              │
     │              │ 7. 抢购结果  │              │              │
     │◀────────────│─────────────│              │              │
     │              │              │              │              │
     │              │              │ 8. 发送消息  │              │
     │              │              │──────────────│─────────────▶│
     │              │              │              │              │
     │              │              │         9. 消费者处理订单    │
     │              │              │              │              │
     │              │              │              │              │
┌────┴────┐    ┌────┴────┐    ┌────┴────┐    ┌────┴────┐    ┌────┴────┐
│  用户   │    │  网关   │    │ 秒杀服务 │   │  Redis  │    │  Kafka  │
└─────────┘    └─────────┘    └─────────┘    └─────────┘    └─────────┘
```

### Redis Lua 核心代码

```lua
-- 秒杀扣库存 Lua 脚本
local stockKey = KEYS[1]           -- seckill:stock:{skuId}
local userKey = KEYS[2]            -- seckill:user:{skuId}:{userId}
local orderId = ARGV[1]            -- 订单ID
local userId = ARGV[2]             -- 用户ID

-- 检查是否重复抢购
if redis.call('EXISTS', userKey) == 1 then
    return -1  -- 重复抢购
end

-- 检查库存
local stock = tonumber(redis.call('GET', stockKey))
if stock == nil or stock <= 0 then
    return 0   -- 库存不足
end

-- 扣减库存
redis.call('DECRBY', stockKey, 1)

-- 标记用户已抢购
redis.call('SET', userKey, orderId)
redis.call('EXPIRE', userKey, 3600)  -- 1小时过期

return 1  -- 抢购成功
```

### Kafka 消费者配置

```java
@Configuration
public class KafkaConsumerConfig {

    @Bean
    public ConsumerFactory<String, SeckillOrder> consumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "seckill-order-group");
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 50);  // 批量消费
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        return new DefaultKafkaConsumerFactory<>(props,
            new StringDeserializer(),
            new JsonDeserializer<>(SeckillOrder.class));
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, SeckillOrder>
            kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, SeckillOrder> factory =
            new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        factory.setConcurrency(20);  // 20个消费者线程
        factory.getContainerProperties().setPollTimeout(3000);
        return factory;
    }
}
```

### 库存回滚定时任务

```java
@Component
public class StockRollbackScheduler {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private OrderMapper orderMapper;

    // 每 5 分钟执行一次
    @Scheduled(fixedDelay = 300000)
    public void batchRollbackStock() {
        log.info("开始执行库存回滚任务");

        // 查询超时未支付订单（15分钟）
        List<Order> timeoutOrders = orderMapper.selectTimeoutOrders(
            LocalDateTime.now().minusMinutes(15)
        );

        if (timeoutOrders.isEmpty()) {
            return;
        }

        // 按 SKU 分组统计
        Map<Long, Integer> rollbackMap = new HashMap<>();
        for (Order order : timeoutOrders) {
            rollbackMap.merge(order.getSkuId(), 1, Integer::sum);
        }

        // 批量回滚 Redis 库存
        rollbackMap.forEach((skuId, count) -> {
            String stockKey = "seckill:stock:" + skuId;
            redisTemplate.opsForValue().increment(stockKey, count);
            log.info("回滚库存: skuId={}, count={}", skuId, count);
        });

        // 批量取消订单
        orderMapper.batchCancelOrders(timeoutOrders);

        log.info("库存回滚完成，回滚订单数: {}", timeoutOrders.size());
    }
}
```

### 秒杀数据表设计

```sql
-- 秒杀商品表
CREATE TABLE seckill_sku (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    product_id BIGINT NOT NULL COMMENT '商品ID',
    sku_name VARCHAR(200) NOT NULL COMMENT 'SKU名称',
    original_price DECIMAL(10,2) NOT NULL COMMENT '原价',
    seckill_price DECIMAL(10,2) NOT NULL COMMENT '秒杀价',
    stock_count INT NOT NULL DEFAULT 1000 COMMENT '库存数量',
    start_time DATETIME NOT NULL COMMENT '开始时间',
    end_time DATETIME NOT NULL COMMENT '结束时间',
    status TINYINT DEFAULT 1 COMMENT '状态 1-正常 0-禁用',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_time (start_time, end_time),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='秒杀商品表';

-- 秒杀订单表
CREATE TABLE seckill_order (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_no VARCHAR(64) UNIQUE NOT NULL COMMENT '订单号',
    sku_id BIGINT NOT NULL COMMENT '秒杀SKU ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    status TINYINT DEFAULT 0 COMMENT '0-待支付 1-已支付 2-已取消',
    money DECIMAL(10,2) NOT NULL COMMENT '订单金额',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    pay_time DATETIME COMMENT '支付时间',
    INDEX idx_user (user_id),
    INDEX idx_sku (sku_id),
    INDEX idx_status (status),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='秒杀订单表';
```

---

## AI 服务设计

### RAG 系统架构

```
┌─────────────────────────────────────────────────────────────┐
│                  RAG (检索增强生成) 系统                    │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  用户问题："云南5天游，预算5000"                            │
│      │                                                      │
│      ▼                                                      │
│  ┌─────────────────────────────────────────────────────┐   │
│  │  1. 向量化 (Sentence-Transformers)                   │   │
│  │     问题 → Embedding 向量 (768维)                    │   │
│  └─────────────────────────────────────────────────────┘   │
│      │                                                      │
│      ▼                                                      │
│  ┌─────────────────────────────────────────────────────┐   │
│  │  2. 向量检索 (Milvus)                                │   │
│  │     • 查询景点库（Top 10）                           │   │
│  │     • 查询商品库（Top 10）                           │   │
│  │     • 查询历史行程（相似案例）                       │   │
│  └─────────────────────────────────────────────────────┘   │
│      │                                                      │
│      ▼                                                      │
│  ┌─────────────────────────────────────────────────────┐   │
│  │  3. 构建 Prompt                                      │   │
│  │     system: 你是旅游规划助手...                      │   │
│  │     context: {检索到的景点、商品信息}                │   │
│  │     user: {用户问题}                                 │   │
│  └─────────────────────────────────────────────────────┘   │
│      │                                                      │
│      ▼                                                      │
│  ┌─────────────────────────────────────────────────────┐   │
│  │  4. GLM-5 生成                                       │   │
│  │     • 生成 5 天行程                                  │   │
│  │     • 挂载商品推荐                                  │   │
│  └─────────────────────────────────────────────────────┘   │
│      │                                                      │
│      ▼                                                      │
│  返回结果：JSON 格式行程 + 商品                            │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### GLM-5 服务封装

```java
@Service
public class GLM5Service {

    @Value("${glm5.api.key}")
    private String apiKey;

    @Value("${glm5.api.url}")
    private String apiUrl;

    private final RestTemplate restTemplate;

    public String chat(String prompt, String context) {
        GLM5Request request = GLM5Request.builder()
            .model("glm-5")
            .messages(Arrays.asList(
                new Message("system", "你是智慧旅游助手，擅长规划行程和推荐商品。"),
                new Message("user", context + "\n\n" + prompt)
            ))
            .temperature(0.7)
            .maxTokens(2000)
            .build();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        HttpEntity<GLM5Request> entity = new HttpEntity<>(request, headers);

        ResponseEntity<GLM5Response> response = restTemplate.postForEntity(
            apiUrl, entity, GLM5Response.class
        );

        return response.getBody().getChoices().get(0).getMessage().getContent();
    }

    public TripPlanResponse planTrip(TripPlanRequest request) {
        // 1. 向量化用户需求
        float[] embedding = embeddingService.embed(request.toString());

        // 2. 检索相关景点和商品
        List<Attraction> attractions = milvusService.searchAttractions(embedding, 10);
        List<Product> products = milvusService.searchProducts(embedding, 10);

        // 3. 构建 Context
        String context = buildContext(attractions, products);

        // 4. 调用 GLM-5
        String prompt = buildPrompt(request);
        String response = chat(prompt, context);

        // 5. 解析结果
        return parseTripPlan(response);
    }
}
```

### 向量数据库配置

```java
@Configuration
public class MilvusConfig {

    @Bean
    public MilvusClient milvusClient() {
        ConnectParam connectParam = ConnectParam.newBuilder()
            .withHost("localhost")
            .withPort(19530)
            .build();

        return new MilvusServiceClient(connectParam);
    }

    @Bean
    public CollectionAttr collectionAttr() {
        return CollectionAttr.newBuilder()
            .withCollectionName("travel_attractions")
            .withDimension(768)  // Sentence-Transformers 输出维度
            .withIndexType(IndexType.IVF_FLAT)
            .withMetricType(MetricType.L2)
            .build();
    }
}
```

---

## 数字人直播设计（MVP）

### 简化架构

```
┌─────────────────────────────────────────────────────────────┐
│              数字人直播 MVP（10人并发）                      │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  用户弹幕                                                   │
│      │                                                      │
│      ▼                                                      │
│  ┌─────────────────────────────────────────────────────┐   │
│  │  WebSocket Server (Netty)                           │   │
│  │  • 接收弹幕（10人并发足够）                         │   │
│  │  • 内存队列缓存                                     │   │
│  └─────────────────────────────────────────────────────┘   │
│      │                                                      │
│      ▼                                                      │
│  ┌─────────────────────────────────────────────────────┐   │
│  │  意图识别 + 合并                                     │   │
│  │  • 关键词匹配（价格、时间、优惠）                   │   │
│  │  • 5秒内相似问题合并                                │   │
│  └─────────────────────────────────────────────────────┘   │
│      │                                                      │
│      ▼                                                      │
│  ┌─────────────────────────────────────────────────────┐   │
│  │  GLM-5 生成回复                                      │   │
│  │  • 输入：合并后的问题                                │   │
│  │  • 输出：回复文本                                    │   │
│  └─────────────────────────────────────────────────────┘   │
│      │                                                      │
│      ▼                                                      │
│  ┌─────────────────────────────────────────────────────┐   │
│  │  调用云端 API（阿里云/腾讯云）                       │   │
│  │  • 文本 → 语音 → 视频                                │   │
│  │  • 返回视频 URL                                      │   │
│  └─────────────────────────────────────────────────────┘   │
│      │                                                      │
│      ▼                                                      │
│  WebSocket 推送视频 URL 给前端                             │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### 核心代码

```java
@Service
public class DigitalHumanService {

    @Value("${aliyun.digitalHuman.accessKey}")
    private String accessKey;

    @Value("${aliyun.digitalHuman.appKey}")
    private String appKey;

    /**
     * 生成数字人视频
     * @param text 回复文本
     * @return 视频 URL
     */
    public String generateVideo(String text) {
        try {
            // 调用阿里云数字人 API
            DefaultProfile profile = DefaultProfile.getProfile(
                "cn-hangzhou", accessKey, appKey
            );

            IAcsClient client = new DefaultAcsClient(profile);

            CreateAvatarVideoRequest request = new CreateAvatarVideoRequest();
            request.setAppKey(appKey);
            request.setText(text);
            request.setAvatarId("xiaoxia");  // 数字人形象
            request.setVoiceId("zhixiaoxia"); // 声音

            CreateAvatarVideoResponse response = client.getAcsResponse(request);

            // 异步获取视频结果
            String taskId = response.getTaskId();
            return pollVideoResult(taskId);

        } catch (Exception e) {
            log.error("数字人视频生成失败", e);
            return null;
        }
    }

    private String pollVideoResult(String taskId) throws InterruptedException {
        for (int i = 0; i < 30; i++) {  // 最多等30秒
            Thread.sleep(1000);

            QueryAvatarVideoRequest request = new QueryAvatarVideoRequest();
            request.setTaskId(taskId);

            QueryAvatarVideoResponse response = client.getAcsResponse(request);

            if ("SUCCESS".equals(response.getStatus())) {
                return response.getVideoUrl();
            }
        }
        throw new RuntimeException("视频生成超时");
    }
}
```

---

## 技术栈清单

### 后端技术栈

| 技术 | 版本 | 用途 |
|------|------|------|
| **Spring Boot** | 3.2.0 | 核心框架 |
| **Spring Security** | 6.2.0 | 认证授权 |
| **MyBatis-Plus** | 3.5.5 | ORM 框架 |
| **Redis** | 7.0 | 缓存 + 秒杀库存 |
| **Redisson** | 3.25.0 | 分布式锁 |
| **Kafka** | 3.6.0 | 消息队列 |
| **RabbitMQ** | 3.12.0 | 延迟队列 |
| **PostgreSQL** | 15.0 | 主数据库 |
| **Milvus** | 2.3.0 | 向量数据库 |
| **Sentinel** | 1.8.6 | 限流熔断 |
| **Seata** | 1.7.0 | 分布式事务 |

### 前端技术栈

| 技术 | 版本 | 用途 |
|------|------|------|
| **Vue.js** | 3.3.0 | 前端框架 |
| **Vite** | 5.0.0 | 构建工具 |
| **Pinia** | 2.1.0 | 状态管理 |
| **Element Plus** | 2.4.0 | UI 组件库 |
| **Axios** | 1.6.0 | HTTP 客户端 |

### AI & 云服务

| 服务 | 用途 |
|------|------|
| **GLM-5 API** | AI 对话生成 |
| **Sentence-Transformers** | 文本向量化 |
| **阿里云数字人** | 视频生成 |
| **阿里云 OSS** | 文件存储 |
| **阿里云 CDN** | 内容分发 |
| **支付宝/微信** | 支付接口 |

---

## 部署架构

### 生产环境部署

```
┌─────────────────────────────────────────────────────────────┐
│                       生产环境部署                           │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌───────────────────────────────────────────────────────┐ │
│  │  负载均衡 (ALB/SLB)                                    │ │
│  └───────────────────────────────────────────────────────┘ │
│      │                                                     │
│      ▼                                                     │
│  ┌───────────────────────────────────────────────────────┐ │
│  │  API 网关集群 (3 节点)                                │ │
│  │  • Nginx + Spring Cloud Gateway                       │ │
│  └───────────────────────────────────────────────────────┘ │
│      │                                                     │
│      ▼                                                     │
│  ┌──────────┬──────────┬──────────┬──────────┬──────────┐ │
│  │ 用户服务 │ 商品服务 │ 订单服务 │ 秒杀服务 │ AI服务   │ │
│  │ (3节点)  │ (3节点)  │ (3节点)  │ (5节点)  │ (2节点)  │ │
│  └──────────┴──────────┴──────────┴──────────┴──────────┘ │
│                                                             │
│  ┌───────────────────────────────────────────────────────┐ │
│  │  中间件层                                              │ │
│  │  • Redis Sentinel (3 节点)                            │ │
│  │  • Kafka Cluster (3 节点)                             │ │
│  │  • RabbitMQ Cluster (3 节点)                          │ │
│  └───────────────────────────────────────────────────────┘ │
│                                                             │
│  ┌───────────────────────────────────────────────────────┐ │
│  │  数据层                                                │ │
│  │  • PostgreSQL 主从 (1 主 2 从)                        │ │
│  │  • Milvus 集群 (3 节点)                               │ │
│  └───────────────────────────────────────────────────────┘ │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

---

## 性能优化策略

### 秒杀性能优化

| 优化点 | 方案 | 预期效果 |
|--------|------|---------|
| **Redis 预热** | 提前加载库存到 Redis | 减少数据库压力 |
| **本地缓存** | Guava Cache 缓存商品信息 | 减少 Redis 查询 |
| **批量处理** | Kafka 批量消费 | 提升吞吐量 5 倍 |
| **连接池优化** | HikariCP 连接池 | 减少连接创建开销 |
| **异步处理** | 订单创建异步化 | 响应时间减少 80% |

### 数据库优化

```sql
-- 索引优化
CREATE INDEX idx_user_status ON seckill_order(user_id, status);
CREATE INDEX idx_sku_time ON seckill_sku(start_time, end_time);

-- 分区表（按月分区）
ALTER TABLE seckill_order PARTITION BY RANGE (YEAR(create_time) * 100 + MONTH(create_time));
```

---

## 监控与告警

### 监控指标

```
┌─────────────────────────────────────────────────────────────┐
│                      监控指标体系                           │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  应用层监控                                                 │
│  • QPS、响应时间、错误率                                   │
│  • JVM 内存、GC、线程数                                    │
│                                                             │
│  中间件监控                                                 │
│  • Redis 命中率、内存使用                                  │
│  • Kafka 消息堆积、消费延迟                                │
│                                                             │
│  业务监控                                                   │
│  • 秒杀成功率、订单转化率                                  │
│  • AI 响应准确率、用户满意度                               │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### 告警规则

| 指标 | 阈值 | 级别 | 处理措施 |
|------|------|------|---------|
| 接口错误率 | > 5% | P1 | 立即回滚 |
| 秒杀响应时间 | > 2s | P2 | 扩容 |
| Redis 内存 | > 80% | P2 | 清理过期数据 |
| Kafka 堆积 | > 10000 | P2 | 增加消费者 |

---

## 安全设计

### 安全防护清单

- [ ] JWT Token 认证
- [ ] API 接口签名验证
- [ ] SQL 注入防护（MyBatis 参数化）
- [ ] XSS 防护（输入过滤 + 输出编码）
- [ ] CSRF 防护（Token 验证）
- [ ] 接口限流（Sentinel）
- [ ] 敏感数据加密（AES-256）
- [ ] 访问日志审计

---

## 后续优化方向

### Phase 2 优化
- 秒杀分片（按 SKU 分片）
- CDN 静态化
- 读写分离
- 缓存预热

### Phase 3 优化
- 微服务拆分
- 服务网格 (Istio)
- 全链路压测
- 灰度发布
