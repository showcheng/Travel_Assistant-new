# 知识库功能规格

## 📋 功能概述

提供基于向量的知识库管理能力，支持文档上传、向量化、智能检索和知识管理。

## 🎯 功能需求

### 1. 文档管理

#### 1.1 文档上传

**功能描述**: 上传旅游相关文档到知识库

**用户故事**:
```
作为管理员
我想要上传旅游文档到知识库
这样AI就能基于这些文档回答用户问题
```

**功能点**:
- ✅ 支持多种文档格式 (PDF, Word, TXT, Markdown)
- ✅ 文件大小限制 (单个文件 < 10MB)
- ✅ 文件类型验证
- ✅ 上传进度显示
- ✅ 批量上传支持

**API接口**:
```http
POST /api/ai/knowledge/upload
Content-Type: multipart/form-data

file: document.pdf
docType: attraction
location: 北京
category: 景点介绍
title: 故宫博物院介绍

Response 200:
{
  "docId": "doc_123",
  "status": "processing",
  "message": "文档上传成功，正在向量化...",
  "estimatedTime": 30
}
```

**文档类型枚举**:
```java
public enum DocumentType {
    ATTRACTION,   // 景点介绍
    GUIDE,        // 旅游攻略
    FAQ,          // 常见问题
    POLICY        // 政策法规
}
```

**验收标准**:
- [ ] 支持至少4种文档格式
- [ ] 上传成功率 > 95%
- [ ] 进度显示准确
- [ ] 错误提示清晰

---

#### 1.2 文档列表查询

**功能描述**: 查看知识库中的文档列表

**API接口**:
```http
GET /api/ai/knowledge/list
Query Params:
  - page: 0
  - size: 10
  - docType: attraction (可选)
  - location: 北京 (可选)
  - status: active (可选)

Response 200:
{
  "total": 50,
  "documents": [
    {
      "docId": "doc_123",
      "title": "故宫博物院介绍",
      "docType": "attraction",
      "location": "北京",
      "category": "历史文化",
      "status": "active",
      "vectorized": true,
      "chunkCount": 15,
      "createdAt": "2026-04-16T14:30:00",
      "updatedAt": "2026-04-16T15:00:00"
    }
  ]
}
```

**验收标准**:
- [ ] 支持分页查询
- [ ] 支持多条件过滤
- [ ] 查询响应时间 < 500ms
- [ ] 数据显示准确

---

#### 1.3 文档详情查看

**功能描述**: 查看文档详细信息和向量化状态

**API接口**:
```http
GET /api/ai/knowledge/{docId}

Response 200:
{
  "docId": "doc_123",
  "title": "故宫博物院介绍",
  "docType": "attraction",
  "location": "北京",
  "category": "历史文化",
  "content": "故宫博物院是中国...",
  "status": "active",
  "vectorized": true,
  "vectorizationStatus": "completed",
  "chunkCount": 15,
  "embeddingModel": "text-embedding-v3",
  "createdAt": "2026-04-16T14:30:00",
  "updatedAt": "2026-04-16T15:00:00"
}
```

**验收标准**:
- [ ] 文档信息完整
- [ ] 向量化状态准确
- [ ] 支持内容预览

---

#### 1.4 文档删除

**功能描述**: 删除不再需要的文档

**API接口**:
```http
DELETE /api/ai/knowledge/{docId}

Response 200:
{
  "success": true,
  "message": "文档删除成功"
}
```

**验收标准**:
- [ ] 删除操作成功
- [ ] 向量数据同步删除
- [ ] 删除确认提示

---

### 2. 文档向量化

#### 2.1 自动向量化

**功能描述**: 文档上传后自动进行向量化处理

**处理流程**:
```
1. 文档上传 → 临时存储
2. 文档预处理 → 清洗、格式化
3. 文档切片 → 按500字左右切片
4. 文本向量化 → 调用Embedding API
5. 向量存储 → 存储到Milvus
6. 状态更新 → 更新文档状态
```

**切片策略**:
```java
public class DocumentSplitter {
    
    // 切片参数
    private static final int CHUNK_SIZE = 500;
    private static final int CHUNK_OVERLAP = 50;
    private static final int MIN_CHUNK_SIZE = 100;
    
    public List<String> split(String text) {
        // 1. 按段落分割
        // 2. 在段落边界切片
        // 3. 保持上下文重叠
        // 4. 过滤过短片段
    }
}
```

**验收标准**:
- [ ] 自动处理成功率 > 95%
- [ ] 切片大小合理 (300-700字)
- [ ] 向量化成功率 > 99%
- [ ] 处理时间 < 30秒/文档

---

#### 2.2 向量化状态查询

**功能描述**: 查询文档向量化处理状态

**状态类型**:
```java
public enum VectorizationStatus {
    PENDING,       // 等待处理
    PROCESSING,    // 处理中
    COMPLETED,     // 已完成
    FAILED         // 失败
}
```

**API接口**:
```http
GET /api/ai/knowledge/{docId}/vectorization-status

Response 200:
{
  "docId": "doc_123",
  "status": "processing",
  "progress": 60,
  "currentStep": "向量化处理",
  "chunksCompleted": 9,
  "chunksTotal": 15,
  "estimatedTimeRemaining": 10
}
```

**验收标准**:
- [ ] 状态更新实时
- [ ] 进度显示准确
- [ ] 估计时间合理

---

### 3. 智能检索

#### 3.1 语义检索

**功能描述**: 基于向量相似度的语义检索

**API接口**:
```http
POST /api/ai/knowledge/search
Content-Type: application/json

{
  "query": "北京适合老人的景点有哪些？",
  "topK": 5,
  "filters": {
    "docType": "attraction",
    "location": "北京"
  }
}

Response 200:
{
  "query": "北京适合老人的景点有哪些？",
  "totalResults": 5,
  "results": [
    {
      "docId": "doc_123",
      "title": "故宫博物院介绍",
      "content": "故宫博物院位于北京中心...",
      "score": 0.85,
      "metadata": {
        "location": "北京",
        "category": "历史文化"
      }
    }
  ],
  "searchTime": 150
}
```

**验收标准**:
- [ ] 检索响应时间 < 1秒
- [ ] 检索准确率 > 70%
- [ ] 支持过滤条件
- [ ] 结果排序合理

---

#### 3.2 混合检索

**功能描述**: 结合向量检索和关键词检索

**检索策略**:
```
1. 向量检索 → 获取语义相似文档 (权重: 70%)
2. 关键词检索 → 获取精确匹配文档 (权重: 30%)
3. 结果融合 → 合并和重排序
4. 多样性处理 → 避免结果过于相似
```

**API接口**:
```http
POST /api/ai/knowledge/hybrid-search
Content-Type: application/json

{
  "query": "故宫门票价格",
  "vectorWeight": 0.7,
  "keywordWeight": 0.3,
  "topK": 10,
  "diversity": 0.3
}

Response 200:
{
  "query": "故宫门票价格",
  "totalResults": 10,
  "results": [...],
  "searchStrategy": "hybrid",
  "searchTime": 200
}
```

**验收标准**:
- [ ] 检索效果优于单一方式
- [ ] 响应时间 < 2秒
- [ ] 结果多样性好

---

### 4. 知识库管理

#### 4.1 批量导入

**功能描述**: 批量导入文档到知识库

**API接口**:
```http
POST /api/ai/knowledge/batch-import
Content-Type: multipart/form-data

files: [file1.pdf, file2.doc, file3.txt]
docType: attraction
location: 北京

Response 200:
{
  "batchId": "batch_123",
  "totalFiles": 3,
  "status": "processing",
  "estimatedTime": 90
}
```

**验收标准**:
- [ ] 支持批量上传
- [ ] 进度跟踪准确
- [ ] 错误处理完善

---

#### 4.2 知识库统计

**功能描述**: 查看知识库统计数据

**API接口**:
```http
GET /api/ai/knowledge/statistics

Response 200:
{
  "totalDocuments": 150,
  "vectorizedDocuments": 145,
  "totalChunks": 2175,
  "byDocType": {
    "attraction": 50,
    "guide": 40,
    "faq": 30,
    "policy": 30
  },
  "byLocation": {
    "北京": 60,
    "上海": 40,
    "西安": 30,
    "其他": 20
  },
  "lastUpdated": "2026-04-16T15:00:00"
}
```

**验收标准**:
- [ ] 统计数据准确
- [ ] 实时更新
- [ ] 可视化展示

---

## 📊 技术实现

### 向量数据库Schema

**Milvus Collection定义**:
```javascript
{
  "collectionName": "travel_knowledge",
  "fields": [
    {"name": "id", "type": "Int64", "primary_key": true, "autoID": true},
    {"name": "docId", "type": "VarChar", "max_length": 64},
    {"name": "content", "type": "VarChar", "max_length": 65535},
    {"name": "embedding", "type": "FloatVector", "dimension": 768},
    {"name": "docType", "type": "VarChar", "max_length": 32},
    {"name": "location", "type": "VarChar", "max_length": 100},
    {"name": "category", "type": "VarChar", "max_length": 100},
    {"name": "createdAt", "type": "Int64"}
  ],
  "indexes": [
    {
      "fieldName": "embedding",
      "indexType": "HNSW",
      "params": {
        "M": 16,
        "efConstruction": 256
      }
    }
  ]
}
```

### 文档处理流程

```java
public class DocumentProcessor {
    
    public void process(Document document) {
        // 1. 预处理
        String cleanedText = preprocess(document);
        
        // 2. 切片
        List<String> chunks = split(cleanedText);
        
        // 3. 向量化
        List<float[]> embeddings = embed(chunks);
        
        // 4. 存储
        storeToMilvus(document, chunks, embeddings);
        
        // 5. 更新状态
        updateStatus(document, VectorizationStatus.COMPLETED);
    }
}
```

---

## 🧪 测试要求

### 功能测试
- [ ] 文档上传功能
- [ ] 向量化处理功能
- [ ] 检索功能准确性
- [ ] 批量操作功能
- [ ] 统计功能准确性

### 性能测试
- [ ] 上传速度测试
- [ ] 向量化处理时间
- [ ] 检索响应时间
- [ ] 并发检索测试

### 质量测试
- [ ] 向量化质量评估
- [ ] 检索准确率评估
- [ ] 数据完整性测试

---

## 🎯 验收标准

### 功能完整性
- [ ] 所有文档管理功能实现
- [ ] 向量化流程稳定
- [ ] 检索功能满足需求
- [ ] 管理界面友好

### 性能达标
- [ ] 文档上传时间 < 5秒/MB
- [ ] 向量化处理 < 30秒/文档
- [ ] 检索响应时间 < 1秒
- [ ] 并发检索 > 50 QPS

### 质量标准
- [ ] 向量化成功率 > 95%
- [ ] 检索准确率 > 70%
- [ ] 数据一致性 100%
- [ ] 文档完整准确

---

## 📚 使用示例

### 示例1: 上传景点介绍文档

```bash
# 上传文档
curl -X POST http://localhost:8080/api/ai/knowledge/upload \
  -F "file=@forbidden_city_intro.pdf" \
  -F "docType=attraction" \
  -F "location=北京" \
  -F "category=历史文化" \
  -F "title=故宫博物院介绍"

# 查询处理状态
curl http://localhost:8080/api/ai/knowledge/doc_123/vectorization-status
```

### 示例2: 检索相关文档

```bash
# 语义检索
curl -X POST http://localhost:8080/api/ai/knowledge/search \
  -H "Content-Type: application/json" \
  -d '{
    "query": "北京适合老人的景点",
    "topK": 5
  }'

# 混合检索
curl -X POST http://localhost:8080/api/ai/knowledge/hybrid-search \
  -H "Content-Type: application/json" \
  -d '{
    "query": "故宫门票",
    "vectorWeight": 0.7,
    "keywordWeight": 0.3,
    "topK": 10
  }'
```

---

**规格状态**: ✅ 已完成  
**版本**: 1.0.0  
**最后更新**: 2026-04-16
