# AI智能助手 - 技术设计

## 🏗️ 系统架构

### 整体架构图

```
┌─────────────────────────────────────────────────────────┐
│                    AI智能助手架构                         │
├─────────────────────────────────────────────────────────┤
│                                                         │
│  🌐 前端层                                  │
│  ├─ 对话界面 (Vue 3 + Element Plus)                    │
│  ├─ WebSocket 客户端                                    │
│  ├─ HTTP API 调用                                       │
│  └─ 历史记录管理                                        │
│              ↕                                           │
│  🤖 AI服务                    │
│  ├─ WebSocket Handler                                   │
│  ├─ 对话管理器 (ConversationManager)                    │
│  ├─ 意图识别器 (IntentRecognizer)                       │
│  ├─ GLM-5 集成器 (GLM5Service)                          │
│  ├─ RAG 检索器 (RAGRetriever)                           │
│  └─ 业务集成器 (BusinessIntegration)                     │
│              ↕                                           │
│  💾 存储层                                              │
│  ├─ Redis (会话缓存 + 对话历史)                         │
│  ├─ Milvus (向量数据库)                                 │
│  └─ MySQL (业务数据)                                    │
│              ↕                                           │
│  🔌 外部服务                                            │
│  ├─ GLM-5 API (智谱AI)                                  │
│  ├─ 商品服务 (Product Service)                          │
│  └─ 订单服务 (Order Service)                            │
│                                                         │
└─────────────────────────────────────────────────────────┘
```

### 服务职责划分

#### travel-ai 服务职责

```java
AI服务核心职责：
├─ 对话管理
│  ├─ 会话生命周期管理
│  ├─ 对话历史存储与检索
│  ├─ 上下文维护与理解
│  └─ 会话状态管理
│
├─ AI能力
│  ├─ GLM-5 对话生成
│  ├─ 意图识别与路由
│  ├─ RAG 知识库检索
│  └─ 多轮对话理解
│
├─ 业务集成
│  ├─ 商品推荐引擎
│  ├─ 订单查询服务
│  ├─ 用户画像集成
│  └─ 数据检索服务
│
└─ 接口层
   ├─ WebSocket 实时接口
   ├─ REST API
   └─ Feign Client (被其他服务调用)
```

### 技术栈选型

| 组件 | 技术选型 | 版本 | 说明 |
|------|----------|------|------|
| **服务框架** | Spring Boot | 3.2.0 | 微服务基础框架 |
| **AI框架** | LangChain4j | 0.34.0 | Java AI编排框架 |
| **大模型** | GLM-5 | - | 智谱AI大模型 |
| **向量数据库** | Milvus | 2.3.0 | 向量存储与检索 |
| **缓存** | Redis | 7.0 | 会话缓存 |
| **数据库** | MySQL | 8.0 | 业务数据存储 |
| **前端框架** | Vue 3 | 3.4.0 | 前端框架 |
| **UI组件** | Element Plus | 2.5.0 | UI组件库 |
| **通信协议** | WebSocket | - | 实时双向通信 |

## 💬 对话管理系统设计

### 会话数据结构

```java
/**
 * 对话会话实体
 */
@Entity
@Table(name = "conversation_session")
@Data
public class ConversationSession {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false, length = 64)
    private String sessionId;        // 会话唯一ID
    
    @Column(nullable = false)
    private Long userId;             // 用户ID
    
    @Column(length = 255)
    private String title;            // 会话标题
    
    @Column(length = 20)
    @Enumerated(EnumType.STRING)
    private SessionState state;      // 会话状态
    
    @Column(defaultValue = "0")
    private Integer messageCount;    // 消息数量
    
    @Column(length = 100)
    private String currentIntent;    // 当前意图
    
    @Column(columnDefinition = "TEXT")
    private String contextSummary;   // 上下文摘要
    
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Column(name = "last_active_time")
    private LocalDateTime lastActiveTime;
}

/**
 * 会话状态枚举
 */
public enum SessionState {
    ACTIVE,      // 活跃状态
    IDLE,        // 空闲状态 (5分钟无交互)
    CLOSED       // 已关闭
}

/**
 * 对话消息实体
 */
@Entity
@Table(name = "conversation_message")
@Data
public class ConversationMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, length = 64)
    private String sessionId;        // 会话ID
    
    @Column(length = 10, nullable = false)
    @Enumerated(EnumType.STRING)
    private MessageRole role;        // 角色 (user/assistant/system)
    
    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;          // 消息内容
    
    @Column(defaultValue = "0")
    private Integer tokens;          // Token使用量
    
    @Column(length = 50)
    private String intent;           // 意图
    
    @Column(columnDefinition = "JSON")
    private String entities;         // 提取的实体 (JSON格式)
    
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}

/**
 * 消息角色枚举
 */
public enum MessageRole {
    USER,        // 用户消息
    ASSISTANT,   // AI助手消息
    SYSTEM       // 系统消息
}
```

### 会话管理器设计

```java
/**
 * 对话管理器
 */
@Component
@Slf4j
public class ConversationManager {
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    @Autowired
    private ConversationSessionRepository sessionRepository;
    
    private static final String SESSION_KEY_PREFIX = "session:context:";
    private static final int SESSION_TTL_HOURS = 24;
    
    /**
     * 创建新会话
     */
    public ConversationSession createSession(Long userId) {
        ConversationSession session = new ConversationSession();
        session.setSessionId(generateSessionId());
        session.setUserId(userId);
        session.setState(SessionState.ACTIVE);
        session.setMessageCount(0);
        session.setCreatedAt(LocalDateTime.now());
        session.setLastActiveTime(LocalDateTime.now());
        
        // 保存到数据库
        sessionRepository.save(session);
        
        // 缓存会话信息
        cacheSession(session);
        
        return session;
    }
    
    /**
     * 获取会话上下文
     */
    public ConversationContext getContext(String sessionId) {
        // 1. 从Redis缓存获取
        String key = SESSION_KEY_PREFIX + sessionId;
        ConversationContext context = (ConversationContext) redisTemplate.opsForValue().get(key);
        
        if (context == null) {
            // 2. 从数据库重建
            context = rebuildContext(sessionId);
            // 3. 回填缓存
            cacheContext(sessionId, context);
        }
        
        return context;
    }
    
    /**
     * 添加消息到会话
     */
    public void addMessage(String sessionId, ConversationMessage message) {
        // 1. 保存到数据库
        messageRepository.save(message);
        
        // 2. 更新会话信息
        ConversationSession session = getSession(sessionId);
        session.setMessageCount(session.getMessageCount() + 1);
        session.setLastActiveTime(LocalDateTime.now());
        sessionRepository.save(session);
        
        // 3. 更新Redis缓存
        updateRecentContext(sessionId, message);
    }
    
    /**
     * 构建对话上下文 (简化版)
     */
    private ConversationContext buildContext(String sessionId) {
        ConversationContext context = new ConversationContext();
        
        // 1. 获取最近3轮对话
        List<ConversationMessage> recentMessages = getRecentMessages(sessionId, 3);
        context.setRecentMessages(recentMessages);
        
        // 2. 获取会话信息
        ConversationSession session = getSession(sessionId);
        context.setSessionInfo(session);
        
        // 3. 简单的指代消解
        if (!recentMessages.isEmpty()) {
            ConversationMessage lastMessage = recentMessages.get(recentMessages.size() - 1);
            context.setLastEntity(extractMainEntity(lastMessage));
        }
        
        return context;
    }
    
    /**
     * 会话缓存
     */
    private void cacheSession(ConversationSession session) {
        String key = SESSION_KEY_PREFIX + session.getSessionId();
        redisTemplate.opsForValue().set(key, session, SESSION_TTL_HOURS, TimeUnit.HOURS);
    }
    
    private void cacheContext(String sessionId, ConversationContext context) {
        String key = SESSION_KEY_PREFIX + sessionId + ":context";
        redisTemplate.opsForValue().set(key, context, 1, TimeUnit.HOURS);
    }
}
```

### 意图识别系统

```java
/**
 * 意图类型
 */
public enum IntentType {
    // 对话意图
    GREETING,           // 问候
    FAQ,                // 常见问题
    COMPLIMENT,         // 赞美
    COMPLAINT,          // 投诉
    
    // 业务意图
    PRODUCT_RECOMMENDATION,  // 商品推荐
    ORDER_QUERY,            // 订单查询
    PRICE_INQUIRY,          // 价格询问
    
    // 旅游意图
    ATTRACTION_QUERY,   // 景点查询
    ROUTE_PLANNING,     // 路线规划
    WEATHER_QUERY,      // 天气查询
    
    // 未知意图
    UNKNOWN
}

/**
 * 意图识别器
 */
@Component
@Slf4j
public class IntentRecognizer {
    
    /**
     * 识别用户意图 (简化版 - 基于规则)
     */
    public Intent recognizeIntent(String userMessage, ConversationContext context) {
        Intent intent = new Intent();
        
        // 1. 关键词匹配
        String lowerMessage = userMessage.toLowerCase();
        
        // 问候意图
        if (containsAny(lowerMessage, "你好", "您好", "hello", "hi")) {
            intent.setType(IntentType.GREETING);
            intent.setConfidence(0.9);
            return intent;
        }
        
        // 商品推荐意图
        if (containsAny(lowerMessage, "推荐", "有什么好玩的", "哪个好")) {
            intent.setType(IntentType.PRODUCT_RECOMMENDATION);
            intent.setConfidence(0.8);
            return intent;
        }
        
        // 订单查询意图
        if (containsAny(lowerMessage, "订单", "我的订单", "查询订单")) {
            intent.setType(IntentType.ORDER_QUERY);
            intent.setConfidence(0.85);
            return intent;
        }
        
        // 价格询问意图
        if (containsAny(lowerMessage, "多少钱", "价格", "费用")) {
            intent.setType(IntentType.PRICE_INQUIRY);
            intent.setConfidence(0.75);
            return intent;
        }
        
        // 景点查询意图
        if (containsAny(lowerMessage, "景点", "好玩", "去哪")) {
            intent.setType(IntentType.ATTRACTION_QUERY);
            intent.setConfidence(0.8);
            return intent;
        }
        
        // 默认为未知意图
        intent.setType(IntentType.UNKNOWN);
        intent.setConfidence(0.5);
        
        return intent;
    }
    
    /**
     * 提取实体 (简化版)
     */
    public Map<String, Object> extractEntities(String userMessage, Intent intent) {
        Map<String, Object> entities = new HashMap<>();
        
        switch (intent.getType()) {
            case ATTRACTION_QUERY:
                // 提取地点实体
                String location = extractLocation(userMessage);
                if (location != null) {
                    entities.put("location", location);
                }
                break;
                
            case PRODUCT_RECOMMENDATION:
                // 提取偏好实体
                String preference = extractPreference(userMessage);
                if (preference != null) {
                    entities.put("preference", preference);
                }
                break;
                
            case ORDER_QUERY:
                // 提取时间实体
                String timeRange = extractTimeRange(userMessage);
                if (timeRange != null) {
                    entities.put("timeRange", timeRange);
                }
                break;
        }
        
        return entities;
    }
}
```

## 🔍 RAG知识库设计

### 向量数据库Schema设计

```java
/**
 * Milvus Collection Schema
 */
public class MilvusSchema {
    
    /*
    Collection: travel_knowledge
    Fields:
    - id: Int64 (Primary Key)
    - doc_id: Varchar(64) - 文档ID
    - content: Varchar(65535) - 文档内容
    - embedding: FloatVector(768) - 向量表示
    - doc_type: Varchar(32) - 文档类型
    - location: Varchar(100) - 地点标签
    - category: Varchar(100) - 分类标签
    - created_at: Int64 - 创建时间戳
    
    Indexes:
    - embedding: HNSW (M=16, efConstruction=256)
    - created_at: INVERTED
    */
}

/**
 * 文档向量实体
 */
@Data
@Builder
public class DocumentVector {
    private Long id;
    private String docId;
    private String content;
    private float[] embedding;
    private String docType;       // attraction, guide, faq, policy
    private String location;      // 北京, 上海, 西安
    private String category;      // 历史文化, 自然风光
    private Long createdAt;
    private Map<String, Object> metadata;
}

/**
 * 文档类型枚举
 */
public enum DocumentType {
    ATTRACTION,   // 景点介绍
    GUIDE,        // 旅游攻略
    FAQ,          // 常见问题
    POLICY        // 政策法规
}
```

### 文档向量化服务

```java
/**
 * 文档向量化服务
 */
@Component
@Slf4j
public class DocumentVectorizationService {
    
    @Autowired
    private EmbeddingModel embeddingModel;
    
    @Autowired
    private MilvusClient milvusClient;
    
    private static final int CHUNK_SIZE = 500;
    private static final int CHUNK_OVERLAP = 50;
    
    /**
     * 处理文档向量化
     */
    public void processDocument(Document document) {
        try {
            // 1. 文档预处理
            String cleanedText = preprocessDocument(document.getContent());
            
            // 2. 文档切片
            List<String> chunks = splitDocument(cleanedText, CHUNK_SIZE, CHUNK_OVERLAP);
            
            log.info("文档 {} 切片完成，共 {} 个片段", document.getTitle(), chunks.size());
            
            // 3. 向量化并存储
            List<DocumentVector> vectors = new ArrayList<>();
            for (int i = 0; i < chunks.size(); i++) {
                String chunk = chunks.get(i);
                
                // 向量化
                float[] embedding = embeddingModel.embed(chunk);
                
                // 构建向量对象
                DocumentVector vector = DocumentVector.builder()
                    .docId(document.getDocId())
                    .content(chunk)
                    .embedding(embedding)
                    .docType(document.getDocType())
                    .location(document.getLocation())
                    .category(document.getCategory())
                    .createdAt(System.currentTimeMillis())
                    .metadata(buildMetadata(document, i))
                    .build();
                
                vectors.add(vector);
            }
            
            // 4. 批量插入Milvus
            milvusClient.insert("travel_knowledge", vectors);
            
            log.info("文档 {} 向量化完成", document.getTitle());
            
        } catch (Exception e) {
            log.error("文档向量化失败: {}", document.getTitle(), e);
            throw new RuntimeException("文档向量化失败", e);
        }
    }
    
    /**
     * 文档切片
     */
    private List<String> splitDocument(String text, int chunkSize, int overlap) {
        List<String> chunks = new ArrayList<>();
        int start = 0;
        
        while (start < text.length()) {
            int end = Math.min(start + chunkSize, text.length());
            String chunk = text.substring(start, end);
            
            // 确保在段落边界切片
            if (end < text.length()) {
                int lastPeriod = chunk.lastIndexOf('。');
                if (lastPeriod > chunkSize / 2) {
                    end = start + lastPeriod + 1;
                    chunk = text.substring(start, end);
                }
            }
            
            chunks.add(chunk.trim());
            start = end - overlap;
        }
        
        return chunks;
    }
    
    /**
     * 文档预处理
     */
    private String preprocessDocument(String content) {
        // 1. 移除特殊字符
        content = content.replaceAll("[\\r\\n]+", "\n");
        
        // 2. 移除多余空格
        content = content.replaceAll("\\s+", " ");
        
        // 3. 移除HTML标签
        content = content.replaceAll("<[^>]+>", "");
        
        return content.trim();
    }
}
```

### RAG检索器

```java
/**
 * RAG检索器
 */
@Component
@Slf4j
public class RAGRetriever {
    
    @Autowired
    private MilvusClient milvusClient;
    
    @Autowired
    private EmbeddingModel embeddingModel;
    
    private static final int TOP_K = 5;
    
    /**
     * 检索相关文档
     */
    public List<Document> retrieveDocuments(String query, RetrievalConfig config) {
        try {
            // 1. 查询向量化
            float[] queryVector = embeddingModel.embed(query);
            
            // 2. 构建检索参数
            SearchParam searchParam = SearchParam.builder()
                .collectionName("travel_knowledge")
                .vectorFieldName("embedding")
                .vectors(Collections.singletonList(queryVector))
                .topK(config.getTopK() != null ? config.getTopK() : TOP_K)
                .expr(buildFilterExpr(config))
                .build();
            
            // 3. 执行检索
            SearchResponse response = milvusClient.search(searchParam);
            
            // 4. 解析结果
            List<Document> documents = parseSearchResults(response);
            
            log.info("检索完成，查询: {}, 结果数: {}", query, documents.size());
            
            return documents;
            
        } catch (Exception e) {
            log.error("RAG检索失败: {}", query, e);
            return Collections.emptyList();
        }
    }
    
    /**
     * 构建过滤表达式
     */
    private String buildFilterExpr(RetrievalConfig config) {
        List<String> filters = new ArrayList<>();
        
        if (config.getDocType() != null) {
            filters.add("doc_type == '" + config.getDocType() + "'");
        }
        
        if (config.getLocation() != null) {
            filters.add("location == '" + config.getLocation() + "'");
        }
        
        return filters.isEmpty() ? "" : String.join(" && ", filters);
    }
    
    /**
     * 解析检索结果
     */
    private List<Document> parseSearchResults(SearchResponse response) {
        // 实现结果解析逻辑
        // 返回Document对象列表
        return new ArrayList<>();
    }
}

/**
 * 检索配置
 */
@Data
@Builder
public class RetrievalConfig {
    private Integer topK;
    private String docType;
    private String location;
    private String category;
    private Double minScore;
}
```

## 🎯 业务集成设计

### 商品推荐引擎

```java
/**
 * 商品推荐服务
 */
@Component
@Slf4j
public class ProductRecommendationService {
    
    @Autowired
    private ProductClient productClient;
    
    @Autowired
    private UserProfileClient userProfileClient;
    
    /**
     * 推荐商品 (简化版 - 基于规则)
     */
    public List<Product> recommendProducts(Long userId, String query, Intent intent) {
        
        // 1. 获取用户偏好
        UserProfile userProfile = userProfileClient.getProfile(userId);
        
        // 2. 构建查询条件
        ProductQuery productQuery = buildQuery(query, intent, userProfile);
        
        // 3. 调用商品服务
        List<Product> products = productClient.searchProducts(productQuery);
        
        // 4. 排序和过滤
        List<Product> ranked = rankProducts(products, userProfile);
        
        // 5. 限制返回数量
        return ranked.stream()
            .limit(10)
            .collect(Collectors.toList());
    }
    
    /**
     * 构建查询条件
     */
    private ProductQuery buildQuery(String query, Intent intent, UserProfile userProfile) {
        ProductQuery.Builder builder = ProductQuery.builder();
        
        // 基于意图的查询
        switch (intent.getType()) {
            case PRODUCT_RECOMMENDATION:
                // 使用用户偏好
                if (userProfile.getPreferredLocation() != null) {
                    builder.location(userProfile.getPreferredLocation());
                }
                if (userProfile.getPreferredCategory() != null) {
                    builder.category(userProfile.getPreferredCategory());
                }
                break;
                
            case PRICE_INQUIRY:
                // 价格敏感型推荐
                builder.maxPrice(userProfile.getBudget() != null ? 
                    userProfile.getBudget() : BigDecimal.valueOf(500));
                break;
                
            case ATTRACTION_QUERY:
                // 景点推荐
                builder.category("景点门票");
                if (userProfile.getPreferredLocation() != null) {
                    builder.location(userProfile.getPreferredLocation());
                }
                break;
        }
        
        // 通用条件
        builder.inStock(true)
              .status(ProductStatus.ACTIVE);
        
        return builder.build();
    }
    
    /**
     * 商品排序 (简化版)
     */
    private List<Product> rankProducts(List<Product> products, UserProfile userProfile) {
        // 简单的排序逻辑
        return products.stream()
            .sorted((p1, p2) -> {
                // 1. 优先考虑库存
                int stockCompare = p2.getStock().compareTo(p1.getStock());
                if (stockCompare != 0) return stockCompare;
                
                // 2. 考虑价格因素
                if (userProfile.getBudget() != null) {
                    double priceDiff1 = Math.abs(p1.getPrice().doubleValue() - userProfile.getBudget().doubleValue());
                    double priceDiff2 = Math.abs(p2.getPrice().doubleValue() - userProfile.getBudget().doubleValue());
                    return Double.compare(priceDiff1, priceDiff2);
                }
                
                // 3. 默认按价格排序
                return p1.getPrice().compareTo(p2.getPrice());
            })
            .collect(Collectors.toList());
    }
}
```

### 订单查询服务

```java
/**
 * 订单查询服务
 */
@Component
@Slf4j
public class OrderQueryService {
    
    @Autowired
    private OrderClient orderClient;
    
    /**
     * 自然语言订单查询
     */
    public OrderQueryResult queryOrders(Long userId, String naturalQuery) {
        
        // 1. 提取查询条件
        OrderQueryCondition condition = extractQueryConditions(naturalQuery);
        condition.setUserId(userId);
        
        // 2. 调用订单服务
        List<Order> orders = orderClient.queryOrders(condition);
        
        // 3. 格式化结果
        return formatOrderResult(orders, naturalQuery);
    }
    
    /**
     * 提取查询条件 (简化版 - 关键词匹配)
     */
    private OrderQueryCondition extractQueryConditions(String query) {
        OrderQueryCondition condition = new OrderQueryCondition();
        
        String lowerQuery = query.toLowerCase();
        
        // 时间范围提取
        if (lowerQuery.contains("最近")) {
            condition.setTimeRange(TimeRange.RECENT_7_DAYS);
        } else if (lowerQuery.contains("本月")) {
            condition.setTimeRange(TimeRange.THIS_MONTH);
        } else if (lowerQuery.contains("今年")) {
            condition.setTimeRange(TimeRange.THIS_YEAR);
        }
        
        // 订单状态提取
        if (lowerQuery.contains("未支付")) {
            condition.setStatus(OrderStatus.PENDING_PAYMENT);
        } else if (lowerQuery.contains("已完成")) {
            condition.setStatus(OrderStatus.COMPLETED);
        } else if (lowerQuery.contains("已取消")) {
            condition.setStatus(OrderStatus.CANCELLED);
        }
        
        return condition;
    }
    
    /**
     * 格式化订单结果
     */
    private OrderQueryResult formatOrderResult(List<Order> orders, String query) {
        OrderQueryResult result = new OrderQueryResult();
        result.setQuery(query);
        result.setTotalCount(orders.size());
        result.setOrders(orders);
        
        // 生成自然语言描述
        String description = generateDescription(orders, query);
        result.setDescription(description);
        
        return result;
    }
    
    /**
     * 生成结果描述
     */
    private String generateDescription(List<Order> orders, String query) {
        if (orders.isEmpty()) {
            return "抱歉，没有找到相关订单。";
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("为您找到 ").append(orders.size()).append(" 个订单：\n");
        
        for (int i = 0; i < Math.min(3, orders.size()); i++) {
            Order order = orders.get(i);
            sb.append(i + 1).append(". ")
              .append(order.getProductName())
              .append(" - ").append(order.getStatus().getDescription())
              .append(" (").append(order.getCreateTime()).append(")\n");
        }
        
        if (orders.size() > 3) {
            sb.append("还有 ").append(orders.size() - 3).append(" 个订单...");
        }
        
        return sb.toString();
    }
}
```

## ⚡ 性能优化设计

### 缓存策略

```java
/**
 * AI服务缓存配置
 */
@Configuration
@EnableCaching
@Slf4j
public class AICacheConfig {
    
    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory factory) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(10))
            .disableCachingNullValues()
            .serializeKeysWith(RedisSerializationContext.SerializationPair
                .fromSerializer(new StringRedisSerializer()))
            .serializeValuesWith(RedisSerializationContext.SerializationPair
                .fromSerializer(new GenericJackson2JsonRedisSerializer()));
        
        return RedisCacheManager.builder(factory)
            .cacheDefaults(config)
            .build();
    }
}

/**
 * 缓存使用示例
 */
@Service
@Slf4j
public class CachedAIService {
    
    /**
     * LLM响应缓存
     */
    @Cacheable(value = "llm_response", key = "#prompt", unless = "#result == null")
    public String getLLMResponse(String prompt) {
        log.info("LLM缓存未命中，调用API");
        return glm5Service.generate(prompt);
    }
    
    /**
     * 向量检索缓存
     */
    @Cacheable(value = "vector_search", key = "#query.hashCode()", unless = "#result.isEmpty()")
    public List<Document> searchDocuments(String query) {
        log.info("向量检索缓存未命中，执行检索");
        return ragRetriever.retrieveDocuments(query, RetrievalConfig.builder().build());
    }
    
    /**
     * 用户偏好缓存
     */
    @Cacheable(value = "user_profile", key = "#userId")
    public UserProfile getUserProfile(Long userId) {
        log.info("用户偏好缓存未命中，查询数据库");
        return userProfileClient.getProfile(userId);
    }
}
```

### 异步处理

```java
/**
 * 异步消息处理器
 */
@Service
@Slf4j
public class AsyncMessageProcessor {
    
    @Autowired
    private ConversationManager conversationManager;
    
    @Autowired
    private DialogueRouter dialogueRouter;
    
    /**
     * 异步处理用户消息
     */
    @Async("aiTaskExecutor")
    public void processMessageAsync(String sessionId, ChatMessage message) {
        try {
            log.info("异步处理消息: sessionId={}, messageId={}", sessionId, message.getId());
            
            // 1. 获取会话上下文
            ConversationContext context = conversationManager.getContext(sessionId);
            
            // 2. 路由到对应的处理器
            dialogueRouter.route(sessionId, message, context);
            
            // 3. 保存消息
            conversationManager.addMessage(sessionId, 
                ConversationMessage.builder()
                    .sessionId(sessionId)
                    .role(MessageRole.USER)
                    .content(message.getContent())
                    .createdAt(LocalDateTime.now())
                    .build());
            
        } catch (Exception e) {
            log.error("异步处理消息失败: sessionId={}", sessionId, e);
            handleProcessingError(sessionId, e);
        }
    }
    
    /**
     * 错误处理
     */
    private void handleProcessingError(String sessionId, Exception e) {
        // 发送错误消息给用户
        sendErrorMessage(sessionId, "抱歉，处理您的消息时出现错误，请重试。");
        
        // 记录错误日志
        log.error("会话 {} 处理失败: {}", sessionId, e.getMessage());
    }
    
    /**
     * AI任务线程池配置
     */
    @Bean("aiTaskExecutor")
    public Executor aiTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(50);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("ai-processor-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}
```

## 🧪 测试设计

### 单元测试

```java
@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ConversationManagerTest {
    
    @Autowired
    private ConversationManager conversationManager;
    
    private Long testUserId = 1L;
    private String testSessionId;
    
    @Test
    @Order(1)
    public void testCreateSession() {
        ConversationSession session = conversationManager.createSession(testUserId);
        
        assertNotNull(session);
        assertNotNull(session.getSessionId());
        assertEquals(testUserId, session.getUserId());
        assertEquals(SessionState.ACTIVE, session.getState());
        
        testSessionId = session.getSessionId();
    }
    
    @Test
    @Order(2)
    public void testAddMessage() {
        ConversationMessage message = ConversationMessage.builder()
            .sessionId(testSessionId)
            .role(MessageRole.USER)
            .content("北京有什么好玩的？")
            .createdAt(LocalDateTime.now())
            .build();
        
        conversationManager.addMessage(testSessionId, message);
        
        ConversationContext context = conversationManager.getContext(testSessionId);
        assertEquals(1, context.getRecentMessages().size());
    }
    
    @Test
    @Order(3)
    public void testMultiTurnConversation() {
        // 第一轮
        conversationManager.addMessage(testSessionId, 
            buildUserMessage("北京有什么好玩的？"));
        conversationManager.addMessage(testSessionId,
            buildAssistantMessage("北京有很多著名景点..."));
        
        // 第二轮
        conversationManager.addMessage(testSessionId,
            buildUserMessage("故宫门票多少钱？"));
        
        ConversationContext context = conversationManager.getContext(testSessionId);
        assertEquals(3, context.getRecentMessages().size());
    }
}
```

### 集成测试

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public class AIIntegrationTest {
    
    @LocalServerPort
    private int port;
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @Test
    public void testEndToEndDialogue() {
        // 1. 创建会话
        String sessionId = createSession();
        
        // 2. 发送消息
        ChatMessage message = ChatMessage.builder()
            .content("北京有什么好玩的？")
            .build();
        
        ResponseEntity<ChatResponse> response = restTemplate.postForEntity(
            "/api/ai/chat/send/" + sessionId,
            message,
            ChatResponse.class
        );
        
        // 3. 验证响应
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().getContent().contains("北京"));
    }
}
```

## 📊 监控设计

### 指标监控

```java
/**
 * AI指标收集器
 */
@Component
@Slf4j
public class AIMetricsCollector {
    
    @Autowired
    private MeterRegistry meterRegistry;
    
    /**
     * 记录对话指标
     */
    public void recordDialogueMetrics(DialogueSession session) {
        // 响应时间
        meterRegistry.timer("ai.dialogue.response_time")
            .record(session.getAvgResponseTime(), TimeUnit.MILLISECONDS);
        
        // Token使用量
        meterRegistry.counter("ai.dialogue.tokens").increment(session.getTotalTokens());
        
        // 用户满意度
        meterRegistry.gauge("ai.dialogue.satisfaction", session.getSatisfactionScore());
    }
    
    /**
     * 记录检索指标
     */
    public void recordRetrievalMetrics(RetrievalResult result) {
        // 检索延迟
        meterRegistry.timer("ai.retrieval.latency")
            .record(result.getLatency(), TimeUnit.MILLISECONDS);
        
        // 检索准确率
        meterRegistry.gauge("ai.retrieval.accuracy", result.getAccuracy());
        
        // 召回率
        meterRegistry.gauge("ai.retrieval.recall", result.getRecall());
    }
}
```

---

**设计文档状态**: ✅ 已完成
**版本**: 1.0.0
**最后更新**: 2026-04-16
