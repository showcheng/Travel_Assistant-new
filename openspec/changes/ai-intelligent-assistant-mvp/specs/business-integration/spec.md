# 业务集成功能规格

## 📋 功能概述

将AI助手与现有业务系统（商品服务、订单服务、用户服务）集成，实现智能业务查询和推荐。

## 🎯 功能需求

### 1. 商品推荐集成

#### 1.1 基于规则的推荐

**功能描述**: 根据用户意图和偏好推荐相关商品

**用户故事**:
```
作为用户
我想要系统根据我的需求推荐合适的商品
这样我能更快找到想要的商品
```

**推荐场景**:
```
场景1: 景点推荐
用户: "推荐几个北京的历史文化景点"
系统: 推荐故宫、天坛、颐和园等

场景2: 价格推荐
用户: "有什么性价比高的旅游产品"
系统: 推荐价格适中、评分高的产品

场景3: 人群推荐
用户: "有什么适合老人的景点"
系统: 推荐无障碍设施、平缓路线的景点
```

**API接口**:
```http
POST /api/ai/recommend/products
Content-Type: application/json

{
  "userId": 1,
  "query": "推荐几个适合老人的北京景点",
  "intent": {
    "type": "PRODUCT_RECOMMENDATION",
    "entities": {
      "location": "北京",
      "targetAudience": "老人",
      "category": "景点"
    }
  }
}

Response 200:
{
  "query": "推荐几个适合老人的北京景点",
  "recommendations": [
    {
      "productId": "prod_123",
      "name": "故宫博物院成人票",
      "price": 60.00,
      "originalPrice": 80.00,
      "stock": 995,
      "score": 4.8,
      "reason": "景点位于市中心，设施完善，适合老年人游览",
      "image": "https://..."
    }
  ],
  "totalCount": 5
}
```

**推荐规则**:
```java
public class ProductRecommendationEngine {
    
    public List<Product> recommend(Long userId, RecommendationRequest request) {
        
        // 1. 获取用户偏好
        UserProfile profile = getUserProfile(userId);
        
        // 2. 构建查询条件
        ProductQuery query = buildQuery(request, profile);
        
        // 3. 调用商品服务
        List<Product> products = productClient.searchProducts(query);
        
        // 4. 排序和过滤
        List<Product> ranked = rankProducts(products, profile, request);
        
        // 5. 限制返回数量
        return ranked.stream()
            .limit(10)
            .collect(Collectors.toList());
    }
}
```

**验收标准**:
- [ ] 推荐相关度 > 65%
- [ ] 推荐响应时间 < 2秒
- [ ] 支持多种推荐场景
- [ ] 推荐理由清晰

---

#### 1.2 个性化推荐

**功能描述**: 基于用户历史行为的个性化推荐

**个性化因素**:
```java
- 用户浏览历史
- 用户订单历史
- 用户偏好设置
- 用户地理位置
- 用户消费能力
```

**API接口**:
```http
GET /api/ai/recommend/personalized
Query Params:
  - userId: 1
  - count: 10

Response 200:
{
  "recommendations": [...],
  "personalizationFactors": {
    "basedOnHistory": true,
    "basedOnPreferences": true,
    "basedOnLocation": true
  }
}
```

**验收标准**:
- [ ] 推荐个性化明显
- [ ] 用户满意度提升
- [ ] 推荐准确率 > 60%

---

### 2. 订单查询集成

#### 2.1 自然语言订单查询

**功能描述**: 支持自然语言查询订单信息

**查询场景**:
```
场景1: 时间范围查询
用户: "我最近的订单有哪些？"
系统: 返回最近7天的订单

场景2: 状态查询
用户: "我的未支付订单"
系统: 返回所有待支付订单

场景3: 综合查询
用户: "今年有多少个已完成订单？"
系统: 统计并返回数量和列表
```

**API接口**:
```http
POST /api/ai/orders/query
Content-Type: application/json

{
  "userId": 1,
  "naturalQuery": "我最近的未支付订单有哪些？",
  "sessionId": "session_123"
}

Response 200:
{
  "query": "我最近的未支付订单有哪些？",
  "interpretation": {
    "timeRange": "recent_7_days",
    "status": "PENDING_PAYMENT",
    "limit": 10
  },
  "results": {
    "totalCount": 2,
    "orders": [
      {
        "orderId": "order_123",
        "orderNo": "20260416123456",
        "productName": "故宫博物院成人票",
        "status": "待支付",
        "amount": 60.00,
        "createdAt": "2026-04-15T10:30:00"
      }
    ]
  },
  "summary": "为您找到2个最近7天内的待支付订单，总金额120元"
}
```

**查询条件提取**:
```java
public class OrderQueryExtractor {
    
    public OrderQueryCondition extract(String naturalQuery) {
        OrderQueryCondition condition = new OrderQueryCondition();
        
        // 1. 时间范围提取
        if (naturalQuery.contains("最近")) {
            condition.setTimeRange(TimeRange.RECENT_7_DAYS);
        } else if (naturalQuery.contains("本月")) {
            condition.setTimeRange(TimeRange.THIS_MONTH);
        }
        
        // 2. 订单状态提取
        if (naturalQuery.contains("未支付")) {
            condition.setStatus(OrderStatus.PENDING_PAYMENT);
        } else if (naturalQuery.contains("已完成")) {
            condition.setStatus(OrderStatus.COMPLETED);
        }
        
        // 3. 数量限制
        if (naturalQuery.contains("所有")) {
            condition.setLimit(null);  // 不限制
        } else {
            condition.setLimit(10);
        }
        
        return condition;
    }
}
```

**验收标准**:
- [ ] 查询理解准确率 > 80%
- [ ] 查询响应时间 < 1秒
- [ ] 支持多种查询场景
- [ ] 结果描述清晰

---

#### 2.2 订单详情查询

**功能描述**: 支持自然语言查询特定订单详情

**API接口**:
```http
POST /api/ai/orders/detail
Content-Type: application/json

{
  "userId": 1,
  "naturalQuery": "我的订单20260416123456怎么样了？",
  "sessionId": "session_123"
}

Response 200:
{
  "query": "我的订单20260416123456怎么样了？",
  "order": {
    "orderId": "order_123",
    "orderNo": "20260416123456",
    "productName": "故宫博物院成人票",
    "status": "已完成",
    "statusDescription": "您已经使用该订单完成了游览",
    "amount": 60.00,
    "paymentTime": "2026-04-15T10:35:00",
    "completeTime": "2026-04-16T09:00:00",
    "tickets": [
      {
        "ticketType": "成人票",
        "quantity": 2,
        "visitDate": "2026-04-16"
      }
    ]
  },
  "summary": "您的订单20260416123456已完成，使用时间是2026年4月16日"
}
```

**验收标准**:
- [ ] 订单号识别准确率 > 90%
- [ ] 订单详情完整
- [ ] 状态描述清晰

---

### 3. 用户服务集成

#### 3.1 用户画像获取

**功能描述**: 获取用户偏好和行为数据，用于个性化推荐

**API接口**:
```http
GET /api/ai/user/{userId}/profile

Response 200:
{
  "userId": 1,
  "preferences": {
    "preferredLocation": "北京",
    "preferredCategory": "历史文化",
    "budget": 500.00,
    "travelStyle": "文化探索"
  },
  "behavior": {
    "viewCount": 15,
    "orderCount": 3,
    "favoriteProducts": ["prod_1", "prod_2"],
    "lastActiveTime": "2026-04-16T15:00:00"
  }
}
```

**验收标准**:
- [ ] 用户数据准确
- [ ] 隐私保护到位
- [ ] 数据获取及时

---

#### 3.2 用户偏好学习

**功能描述**: 从用户对话中学习偏好，持续优化推荐

**学习内容**:
```java
- 感兴趣的地点
- 偏好的活动类型
- 消费能力范围
- 旅行时间偏好
```

**API接口**:
```http
POST /api/ai/user/{userId}/learn-preference
Content-Type: application/json

{
  "sessionId": "session_123",
  "preferences": {
    "location": "北京",
    "category": "历史文化",
    "budget": "500-1000",
    "travelStyle": "家庭游"
  },
  "confidence": 0.75
}

Response 200:
{
  "success": true,
  "message": "偏好已更新"
}
```

**验收标准**:
- [ ] 偏好学习有效
- [ ] 推荐效果提升
- [ ] 隐私安全保护

---

## 🔧 技术实现

### Feign客户端配置

```java
@FeignClient(name = "product-service", url = "${services.product.url}")
public interface ProductClient {
    
    @GetMapping("/api/products/search")
    List<Product> searchProducts(@RequestBody ProductQuery query);
    
    @GetMapping("/api/products/{productId}")
    Product getProduct(@PathVariable Long productId);
}

@FeignClient(name = "order-service", url = "${services.order.url}")
public interface OrderClient {
    
    @GetMapping("/api/orders/search")
    List<Order> searchOrders(@RequestBody OrderQueryCondition condition);
    
    @GetMapping("/api/orders/{orderId}")
    Order getOrder(@PathVariable Long orderId);
}
```

### 服务降级策略

```java
@Component
@Slf4j
public class ServiceCircuitBreaker {
    
    // 商品服务降级
    public List<Product> fallbackProducts(Long userId, RecommendationRequest request) {
        log.warn("商品服务不可用，使用默认推荐");
        
        // 返回热门商品
        return getPopularProducts(request.getLimit());
    }
    
    // 订单服务降级
    public OrderQueryResult fallbackOrderQuery(Long userId, String query) {
        log.warn("订单服务不可用，返回提示信息");
        
        return OrderQueryResult.builder()
            .query(query)
            .summary("抱歉，订单服务暂时不可用，请稍后再试")
            .build();
    }
}
```

---

## 🧪 测试要求

### 集成测试
- [ ] Feign客户端调用测试
- [ ] 服务降级测试
- [ ] 错误处理测试
- [ ] 数据一致性测试

### 功能测试
- [ ] 推荐功能准确性
- [ ] 查询功能准确性
- [ ] 用户体验测试
- [ ] 边界情况测试

### 性能测试
- [ ] 服务调用响应时间
- [ ] 并发处理能力
- [ ] 缓存效果测试

---

## 🎯 验收标准

### 功能完整性
- [ ] 所有集成功能实现
- [ ] 业务流程完整
- [ ] 错误处理完善
- [ ] 降级策略有效

### 性能达标
- [ ] 服务调用 < 500ms
- [ ] 推荐响应 < 2秒
- [ ] 查询响应 < 1秒
- [ ] 降级响应 < 100ms

### 质量标准
- [ ] 集成测试通过率 > 95%
- [ ] 业务准确率 > 80%
- [ ] 用户满意度 > 3.5/5.0

---

## 📚 使用示例

### 示例1: 商品推荐

```bash
curl -X POST http://localhost:8080/api/ai/recommend/products \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1,
    "query": "推荐几个适合老人的北京景点",
    "intent": {
      "type": "PRODUCT_RECOMMENDATION",
      "entities": {
        "location": "北京",
        "targetAudience": "老人"
      }
    }
  }'
```

### 示例2: 订单查询

```bash
curl -X POST http://localhost:8080/api/ai/orders/query \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1,
    "naturalQuery": "我最近的未支付订单有哪些？"
  }'
```

---

**规格状态**: ✅ 已完成  
**版本**: 1.0.0  
**最后更新**: 2026-04-16
