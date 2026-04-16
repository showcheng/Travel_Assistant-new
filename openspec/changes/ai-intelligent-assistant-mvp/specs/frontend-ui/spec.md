# 前端UI功能规格

## 📋 功能概述

设计并实现用户友好的AI对话界面，支持实时对话、历史记录管理和响应式设计。

## 🎨 界面设计

### 整体布局

**页面结构**:
```
┌─────────────────────────────────────────┐
│  智慧旅游助手 - AI对话界面                │
├─────────────────────────────────────────┤
│  [顶部导航栏]                             │
│  - Logo + 标题                            │
│  - 历史记录                               │
│  - 用户头像 + 退出                       │
├─────────────────────────────────────────┤
│  [对话区域]                               │
│  ┌─────────────────────────────────────┐ │
│  │ 助手: 北京有很多著名景点...       │ │
│  │ 用户: 故宫门票多少钱？            │ │
│  │ 助手: 故宫成人票60元...           │ │
│  └─────────────────────────────────────┘ │
├─────────────────────────────────────────┤
│  [输入区域]                               │
│  ┌─────────────────────────────────────┐ │
│  │ [输入框]              [发送按钮]    │ │
│  └─────────────────────────────────────┘ │
└─────────────────────────────────────────┘
```

### 配色方案

```css
/* 主色调 */
--primary-color: #409eff;
--success-color: #67c23a;
--warning-color: #e6a23c;
--danger-color: #f56c6c;

/* 中性色 */
--text-primary: #303133;
--text-regular: #606266;
--text-secondary: #909399;
--border-color: #dcdfe6;

/* 背景色 */
--bg-color: #f5f7fa;
--card-bg: #ffffff;
--hover-bg: #f5f7fa;
```

---

## 💬 对话界面组件

### 1. 对话消息组件

**组件名称**: `ChatMessage.vue`

**功能描述**: 显示单条对话消息

**Props**:
```typescript
interface ChatMessageProps {
  message: {
    id: string;
    role: 'user' | 'assistant' | 'system';
    content: string;
    timestamp: string;
    tokens?: number;
  };
  showAvatar?: boolean;
  showTimestamp?: boolean;
}
```

**模板结构**:
```vue
<template>
  <div :class="['chat-message', `chat-message-${message.role}`]">
    <!-- 用户消息 -->
    <div v-if="message.role === 'user'" class="user-message">
      <div class="message-content">{{ message.content }}</div>
      <div class="message-meta">
        <span class="timestamp">{{ formatTime(message.timestamp) }}</span>
      </div>
    </div>
    
    <!-- AI助手消息 -->
    <div v-else class="assistant-message">
      <div class="message-avatar">
        <el-avatar :size="32" src="/ai-avatar.png" />
      </div>
      <div class="message-content">
        <div class="message-text">{{ message.content }}</div>
        <div class="message-meta">
          <span class="timestamp">{{ formatTime(message.timestamp) }}</span>
          <span v-if="message.tokens" class="tokens">{{ message.tokens }} tokens</span>
        </div>
      </div>
    </div>
  </div>
</template>
```

**样式特点**:
- 用户消息右侧对齐，蓝色背景
- AI消息左侧对齐，白色背景
- 支持Markdown渲染
- 时间戳显示
- Token使用量显示

---

### 2. 消息输入组件

**组件名称**: `ChatInput.vue`

**功能描述**: 用户输入框和发送按钮

**功能点**:
- ✅ 多行文本输入
- ✅ 字符计数显示
- ✅ 快捷键支持 (Enter发送)
- ✅ 发送按钮状态管理
- ✅ 输入框自适应高度

**模板结构**:
```vue
<template>
  <div class="chat-input-container">
    <el-input
      v-model="userInput"
      type="textarea"
      :rows="inputRows"
      :autosize="true"
      :maxlength="1000"
      show-word-limit
      placeholder="输入您的问题... (Enter发送，Shift+Enter换行)"
      @keydown="handleKeydown"
      :disabled="isSending"
    />
    <el-button
      type="primary"
      :loading="isSending"
      :disabled="!canSend"
      @click="sendMessage"
    >
      发送
    </el-button>
  </div>
</template>
```

**交互逻辑**:
```typescript
const handleKeydown = (event: KeyboardEvent) => {
  // Enter发送
  if (event.key === 'Enter' && !event.shiftKey) {
    event.preventDefault();
    sendMessage();
  }
  
  // Shift+Enter换行
  if (event.key === 'Enter' && event.shiftKey) {
    // 默认换行行为
  }
};

const canSend = computed(() => {
  return userInput.value.trim().length > 0 && !isSending.value;
});
```

---

### 3. 对话历史组件

**组件名称**: `ChatHistory.vue`

**功能描述**: 显示和管理对话历史

**功能点**:
- ✅ 会话列表展示
- ✅ 会话搜索
- ✅ 会话删除
- ✅ 会话标题编辑
- ✅ 会话切换

**API集成**:
```typescript
// 获取会话列表
const fetchSessions = async () => {
  const response = await api.getSessions({
    page: currentPage.value,
    size: pageSize
  });
  sessions.value = response.sessions;
  total.value = response.total;
};

// 切换会话
const switchSession = async (sessionId: string) => {
  const response = await api.getSessionMessages(sessionId);
  messages.value = response.messages;
  currentSessionId.value = sessionId;
};

// 删除会话
const deleteSession = async (sessionId: string) => {
  await api.deleteSession(sessionId);
  await fetchSessions(); // 刷新列表
};
```

---

### 4. WebSocket连接管理

**服务名称**: `WebSocketService.ts`

**功能描述**: 管理WebSocket连接和消息收发

**核心功能**:
```typescript
class WebSocketService {
  private ws: WebSocket | null = null;
  private reconnectAttempts = 0;
  private maxReconnectAttempts = 3;
  
  // 连接WebSocket
  connect(sessionId: string): void {
    const wsUrl = `ws://localhost:8080/api/ai/chat/stream/${sessionId}`;
    this.ws = new WebSocket(wsUrl);
    
    this.ws.onopen = () => {
      console.log('WebSocket连接成功');
      this.startHeartbeat();
    };
    
    this.ws.onmessage = (event) => {
      const data = JSON.parse(event.data);
      this.handleMessage(data);
    };
    
    this.ws.onerror = (error) => {
      console.error('WebSocket错误:', error);
    };
    
    this.ws.onclose = () => {
      console.log('WebSocket连接关闭');
      this.reconnect(sessionId);
    };
  }
  
  // 发送消息
  send(message: string): void {
    if (this.ws && this.ws.readyState === WebSocket.OPEN) {
      this.ws.send(JSON.stringify({
        type: 'message',
        content: message,
        timestamp: new Date().toISOString()
      }));
    }
  }
  
  // 心跳检测
  private startHeartbeat(): void {
    setInterval(() => {
      if (this.ws && this.ws.readyState === WebSocket.OPEN) {
        this.ws.send(JSON.stringify({ type: 'ping' }));
      }
    }, 30000); // 每30秒发送一次
  }
  
  // 重连机制
  private reconnect(sessionId: string): void {
    if (this.reconnectAttempts < this.maxReconnectAttempts) {
      const delay = Math.pow(2, this.reconnectAttempts) * 1000; // 1s, 2s, 4s
      setTimeout(() => {
        this.reconnectAttempts++;
        this.connect(sessionId);
      }, delay);
    }
  }
}
```

---

## 🎯 主要页面

### 1. AI对话页面

**路由**: `/ai-chat`

**页面组件**: `AIChatView.vue`

**页面结构**:
```vue
<template>
  <div class="ai-chat-container">
    <!-- 顶部导航 -->
    <div class="chat-header">
      <div class="header-left">
        <el-icon :size="24" color="#409eff"><ChatDotRound /></el-icon>
        <h2>AI智能助手</h2>
      </div>
      <div class="header-center">
        <el-button @click="showHistory" text>
          <el-icon><Clock /></el-icon>
          历史记录
        </el-button>
      </div>
      <div class="header-right">
        <el-dropdown @command="handleUserCommand">
          <el-avatar :size="32" src="{{ userStore.avatar }}">
            {{ userStore.nickname?.charAt(0) }}
          </el-avatar>
          <template #dropdown>
            <el-dropdown-item command="profile">
              <el-icon><User /></el-icon>
              个人中心
            </el-dropdown-item>
            <el-dropdown-item divided command="logout">
              <el-icon><SwitchButton /></el-icon>
              退出登录
            </el-dropdown-item>
          </template>
        </el-dropdown>
      </div>
    </div>
    
    <!-- 对话区域 -->
    <div class="chat-messages" ref="messagesContainer">
      <chat-message
        v-for="message in messages"
        :key="message.id"
        :message="message"
      />
      
      <!-- 加载中提示 -->
      <div v-if="isLoading" class="loading-indicator">
        <el-skeleton :rows="3" animated />
      </div>
    </div>
    
    <!-- 输入区域 -->
    <div class="chat-input-area">
      <chat-input
        @send="handleSendMessage"
        :disabled="isLoading"
      />
    </div>
    
    <!-- 历史记录抽屉 -->
    <el-drawer v-model="historyVisible" title="对话历史" size="400px">
      <chat-history
        @select-session="handleSelectSession"
        @delete-session="handleDeleteSession"
      />
    </el-drawer>
  </div>
</template>
```

---

### 2. 知识库管理页面

**路由**: `/ai-admin/knowledge`

**页面组件**: `KnowledgeManagement.vue`

**主要功能**:
- 文档上传
- 文档列表
- 向量化状态
- 文档搜索
- 文档删除

**页面结构**:
```vue
<template>
  <div class="knowledge-management">
    <!-- 操作栏 -->
    <div class="toolbar">
      <el-button type="primary" @click="showUploadDialog">
        <el-icon><Upload /></el-icon>
        上传文档
      </el-button>
      <el-button @click="refreshList">
        <el-icon><Refresh /></el-icon>
        刷新
      </el-button>
    </div>
    
    <!-- 文档列表 -->
    <el-table :data="documents" v-loading="loading">
      <el-table-column prop="title" label="文档标题" />
      <el-table-column prop="docType" label="文档类型" />
      <el-table-column prop="location" label="地点" />
      <el-table-column prop="chunkCount" label="切片数" />
      <el-table-column label="向量化状态">
        <template #default="{ row }">
          <el-tag :type="getStatusType(row.vectorizationStatus)">
            {{ getStatusText(row.vectorizationStatus) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="200">
        <template #default="{ row }">
          <el-button size="small" @click="viewDetail(row)">查看</el-button>
          <el-button size="small" type="danger" @click="deleteDoc(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    
    <!-- 上传对话框 -->
    <el-dialog v-model="uploadVisible" title="上传文档" width="600px">
      <el-upload
        drag
        action="/api/ai/knowledge/upload"
        :on-success="handleUploadSuccess"
        :file-list="fileList"
        :auto-upload="false"
      >
        <el-icon class="el-icon--upload"><upload-filled /></el-icon>
        <div class="el-upload__text">
          将文件拖到此处，或<em>点击上传</em>
        </div>
        <template #tip>
          <div class="el-upload__tip">
            支持 PDF、Word、TXT 格式，单个文件不超过10MB
          </div>
        </template>
      </el-upload>
      
      <!-- 文档元数据表单 -->
      <el-form :model="docMetadata" label-width="100px">
        <el-form-item label="文档类型">
          <el-select v-model="docMetadata.docType">
            <el-option label="景点介绍" value="attraction" />
            <el-option label="旅游攻略" value="guide" />
            <el-option label="常见问题" value="faq" />
            <el-option label="政策法规" value="policy" />
          </el-select>
        </el-form-item>
        <el-form-item label="地点">
          <el-input v-model="docMetadata.location" />
        </el-form-item>
        <el-form-item label="分类">
          <el-input v-model="docMetadata.category" />
        </el-form-item>
      </el-form>
      
      <template #footer>
        <el-button @click="uploadVisible = false">取消</el-button>
        <el-button type="primary" @click="handleUpload" :loading="uploading">
          确定上传
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>
```

---

## 📱 响应式设计

### 移动端适配

**断点设计**:
```css
/* 大屏幕 (>= 1200px) */
.chat-container {
  max-width: 1200px;
  margin: 0 auto;
}

/* 中等屏幕 (768px - 1199px) */
@media (max-width: 1199px) {
  .chat-container {
    max-width: 100%;
    padding: 10px;
  }
  
  .chat-header {
    padding: 10px;
  }
  
  .chat-messages {
    height: calc(100vh - 200px);
  }
}

/* 小屏幕 (< 768px) */
@media (max-width: 767px) {
  .chat-header {
    padding: 8px;
  }
  
  .header-center {
    display: none;  // 隐藏历史记录按钮
  }
  
  .chat-messages {
    height: calc(100vh - 150px);
    padding: 8px;
  }
  
  .chat-input-area {
    padding: 8px;
  }
  
  .message-content {
    font-size: 14px;  // 调整字体大小
  }
}
```

---

## 🧪 测试要求

### 功能测试
- [ ] 对话发送和接收
- [ ] 流式输出效果
- [ ] 历史记录管理
- [ ] 文档上传功能
- [ ] 响应式适配

### 兼容性测试
- [ ] Chrome浏览器
- [ ] Firefox浏览器
- [ ] Safari浏览器
- [ ] 移动端浏览器

### 性能测试
- [ ] 首屏加载时间 < 3秒
- [ ] 消息发送延迟 < 100ms
- [ ] WebSocket连接稳定

---

## 🎯 验收标准

### 用户体验
- [ ] 界面美观易用
- [ ] 交互流畅自然
- [ ] 响应及时准确
- [ ] 错误提示友好

### 技术实现
- [ ] 组件结构清晰
- [ ] 代码质量达标
- [ ] 性能满足要求
- [ ] 兼容性良好

### 移动端
- [ ] 响应式适配良好
- [ ] 触控交互流畅
- [ ] 字体大小合适
- [ ] 布局不破裂

---

## 📚 使用示例

### 启动对话

```typescript
// 在AIChatView.vue中
const handleSendMessage = async (message: string) => {
  // 1. 添加用户消息到界面
  messages.value.push({
    id: generateId(),
    role: 'user',
    content: message,
    timestamp: new Date().toISOString()
  });
  
  // 2. 滚动到底部
  await nextTick();
  scrollToBottom();
  
  // 3. 显示加载状态
  isLoading.value = true;
  
  // 4. 通过WebSocket发送
  webSocketService.send(message);
};

// 接收AI回复
webSocketService.onMessage((data) => {
  if (data.type === 'token') {
    // 流式输出
    appendToCurrentMessage(data.content);
  } else if (data.type === 'done') {
    // 完成
    isLoading.value = false;
    saveCurrentMessage(data);
  }
});
```

---

**规格状态**: ✅ 已完成  
**版本**: 1.0.0  
**最后更新**: 2026-04-16
