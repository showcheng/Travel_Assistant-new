<template>
  <div class="ai-chat-container">
    <!-- 顶部导航 -->
    <div class="chat-header">
      <div class="header-left">
        <el-icon :size="24" color="#409eff"><ChatDotRound /></el-icon>
        <h2>AI智能助手</h2>
      </div>
      <div class="header-center">
        <el-button @click="showHistory = true" text>
          <el-icon><Clock /></el-icon>
          历史记录
        </el-button>
        <el-button text @click="startNewChat">
          <el-icon><Plus /></el-icon>
          新对话
        </el-button>
      </div>
      <div class="header-right">
        <el-dropdown @command="handleUserCommand">
          <el-avatar :size="32">
            {{ userStore.currentUser?.nickname?.charAt(0) || userStore.currentUser?.phone?.slice(-2) || 'U' }}
          </el-avatar>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="profile">
                <el-icon><User /></el-icon>
                个人中心
              </el-dropdown-item>
              <el-dropdown-item divided command="logout">
                <el-icon><SwitchButton /></el-icon>
                退出登录
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </div>

    <!-- 对话区域 -->
    <div class="chat-messages" ref="messagesContainer">
      <!-- 欢迎消息 -->
      <div v-if="messages.length === 0" class="welcome-message">
        <div class="welcome-content">
          <div class="welcome-icon">
            <el-icon :size="48" color="#409eff"><ChatDotRound /></el-icon>
          </div>
          <h3>欢迎使用AI智能助手</h3>
          <p class="welcome-desc">我可以帮您解答旅游相关问题</p>
          <div class="quick-actions">
            <div
              v-for="action in quickActions"
              :key="action.text"
              class="quick-action-item"
              @click="sendQuickMessage(action.text)"
            >
              <el-icon :size="20" :color="action.color"><component :is="action.icon" /></el-icon>
              <span>{{ action.text }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 消息列表 -->
      <div
        v-for="message in messages"
        :key="message.id"
        :class="['message-item', message.role]"
      >
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
            <el-avatar :size="36" style="background: #409eff">
              <el-icon :size="20" color="#fff"><ChatDotRound /></el-icon>
            </el-avatar>
          </div>
          <div class="message-text">
            <div class="message-content" v-html="renderMarkdown(message.content)"></div>
            <SourceBadge v-if="message.sources" :sources="message.sources" />
            <div class="message-meta">
              <span class="timestamp">{{ formatTime(message.timestamp) }}</span>
              <span v-if="message.tokens" class="tokens">{{ message.tokens }} tokens</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 加载中指示器 -->
      <div v-if="isLoading && !hasStreamingContent" class="loading-indicator">
        <div class="loading-avatar">
          <el-avatar :size="36" style="background: #409eff">
            <el-icon :size="20" color="#fff"><ChatDotRound /></el-icon>
          </el-avatar>
        </div>
        <div class="loading-dots">
          <span></span>
          <span></span>
          <span></span>
        </div>
      </div>
    </div>

    <!-- 输入区域 -->
    <div class="chat-input-area">
      <div class="input-container">
        <el-input
          v-model="userInput"
          type="textarea"
          :autosize="{ minRows: 1, maxRows: 4 }"
          :maxlength="1000"
          show-word-limit
          placeholder="输入您的问题... (Enter发送，Shift+Enter换行)"
          @keydown="handleKeydown"
          :disabled="isLoading"
        />
        <el-button
          type="primary"
          :loading="isLoading"
          :disabled="!canSend"
          @click="sendMessage"
          size="large"
          class="send-btn"
        >
          <el-icon v-if="!isLoading"><Promotion /></el-icon>
          发送
        </el-button>
      </div>
    </div>

    <!-- 历史记录抽屉 -->
    <el-drawer v-model="showHistory" title="对话历史" size="400px">
      <div class="history-content">
        <el-empty v-if="sessions.length === 0" description="暂无历史对话" />
        <div v-else>
          <div
            v-for="session in sessions"
            :key="session.sessionId"
            :class="['history-item', { active: session.sessionId === currentSessionId }]"
            @click="switchSession(session.sessionId)"
          >
            <div class="session-title">{{ session.title || '未命名对话' }}</div>
            <div class="session-meta">
              <span class="message-count">{{ session.messageCount || 0 }} 条消息</span>
              <span class="update-time">{{ formatTime(session.updatedAt) }}</span>
            </div>
          </div>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, nextTick, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ChatDotRound, Clock, User, SwitchButton, Plus, Promotion, MapLocation, Ticket, Guide, Warning } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { http } from '@/utils/request'
import { useStreamChat } from '@/composables/useStreamChat'
import { useMarkdown } from '@/composables/useMarkdown'
import SourceBadge from '@/components/SourceBadge.vue'

const router = useRouter()
const userStore = useUserStore()

const messages = ref<any[]>([])
const userInput = ref('')
const isLoading = ref(false)
const showHistory = ref(false)
const sessions = ref<any[]>([])
const currentSessionId = ref('')

const messagesContainer = ref<HTMLElement>()

const { isStreaming: isStreamActive, streamChat } = useStreamChat()
const { render: renderMarkdown } = useMarkdown()

const quickActions = [
  { text: '推荐热门旅游景点', icon: 'MapLocation', color: '#409eff' },
  { text: '有哪些优惠门票', icon: 'Ticket', color: '#67c23a' },
  { text: '旅游攻略和建议', icon: 'Guide', color: '#e6a23c' },
  { text: '查询我的订单', icon: 'Warning', color: '#f56c6c' }
]

const canSend = computed(() => {
  return userInput.value.trim().length > 0 && !isLoading.value
})

const hasStreamingContent = computed(() => {
  const lastMsg = messages.value[messages.value.length - 1]
  return lastMsg?.role === 'assistant' && lastMsg.content.length > 0
})

const sendMessage = async () => {
  if (!canSend.value) return

  const message = userInput.value.trim()
  userInput.value = ''

  messages.value = [...messages.value, {
    id: Date.now(),
    role: 'user',
    content: message,
    timestamp: new Date().toISOString()
  }]

  await nextTick()
  scrollToBottom()

  isLoading.value = true

  try {
    if (!currentSessionId.value) {
      await createNewSession()
    }

    // Create AI message placeholder for streaming
    const aiMessageId = Date.now() + 1
    messages.value = [...messages.value, {
      id: aiMessageId,
      role: 'assistant',
      content: '',
      timestamp: new Date().toISOString(),
      tokens: 0
    }]

    // Use SSE streaming for real-time response
    await streamChat(
      message,
      currentSessionId.value,
      (chunk) => {
        // Append chunk to the AI message
        const idx = messages.value.findIndex(m => m.id === aiMessageId)
        if (idx >= 0) {
          const updated = [...messages.value]
          updated[idx] = { ...updated[idx], content: updated[idx].content + chunk }
          messages.value = updated
        }
        nextTick(() => scrollToBottom())
      },
      () => {
        // Stream completed successfully
        isLoading.value = false
      },
      (error) => {
        // SSE streaming failed - fall back to regular POST
        fallbackToSend(message, aiMessageId)
      }
    )
  } catch (error) {
    // Unexpected error - show error message
    const aiMessage: any = {
      id: Date.now() + 1,
      role: 'assistant',
      content: '抱歉，AI服务暂时不可用。请确保后端服务已启动，然后再试一次。\n\n您可以尝试：\n1. 检查后端服务是否运行\n2. 刷新页面重试\n3. 开始一个新的对话',
      timestamp: new Date().toISOString()
    }
    messages.value = [...messages.value, aiMessage]
    await nextTick()
    scrollToBottom()
    isLoading.value = false
  }
}

/**
 * Fallback to synchronous POST when SSE streaming fails.
 */
const fallbackToSend = async (message: string, aiMessageId: number) => {
  try {
    const data = await http.post('/api/ai/chat/send', {
      message,
      sessionId: currentSessionId.value,
      userId: userStore.userId || 1
    })
    const idx = messages.value.findIndex(m => m.id === aiMessageId)
    if (idx >= 0) {
      const updated = [...messages.value]
      updated[idx] = {
        ...updated[idx],
        content: data.response || data.data?.response || '抱歉，暂时无法回答。',
        tokens: data.tokens || data.data?.tokens,
        sources: data.sources || data.data?.sources || null
      }
      messages.value = updated
    }
  } catch {
    const idx = messages.value.findIndex(m => m.id === aiMessageId)
    if (idx >= 0) {
      const updated = [...messages.value]
      updated[idx] = {
        ...updated[idx],
        content: '抱歉，AI服务暂时不可用，请稍后再试。'
      }
      messages.value = updated
    }
  } finally {
    isLoading.value = false
  }
}

const sendQuickMessage = (text: string) => {
  userInput.value = text
  sendMessage()
}

const createNewSession = async () => {
  try {
    const data = await http.post('/api/ai/sessions', null, {
      params: { userId: userStore.userId || 1 }
    })
    currentSessionId.value = data.sessionId || data.data || data
    await loadSessions()
  } catch (error) {
    currentSessionId.value = 'local_' + Date.now()
  }
}

const loadSessions = async () => {
  try {
    const data = await http.get('/api/ai/sessions', {
      params: { userId: userStore.userId || 1 }
    })
    sessions.value = data || []
  } catch (error) {
    sessions.value = []
  }
}

const switchSession = async (sessionId: string) => {
  currentSessionId.value = sessionId

  try {
    const data = await http.get(`/api/ai/chat/history/${sessionId}`, {
      params: { limit: 50 }
    })
    messages.value = data || []
  } catch (error) {
    messages.value = []
  }

  showHistory.value = false
}

const startNewChat = () => {
  currentSessionId.value = ''
  messages.value = []
}

const handleKeydown = (event: KeyboardEvent) => {
  if (event.key === 'Enter' && !event.shiftKey) {
    event.preventDefault()
    sendMessage()
  }
}

const scrollToBottom = () => {
  if (messagesContainer.value) {
    messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
  }
}

const formatTime = (timestamp: string) => {
  if (!timestamp) return ''
  const date = new Date(timestamp)
  const now = new Date()
  const diff = now.getTime() - date.getTime()

  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return `${Math.floor(diff / 60000)}分钟前`
  if (diff < 86400000) return `${Math.floor(diff / 3600000)}小时前`
  return date.toLocaleDateString('zh-CN')
}

const handleUserCommand = async (command: string) => {
  if (command === 'profile') {
    router.push('/profile')
  } else if (command === 'logout') {
    try {
      await ElMessageBox.confirm('确定要退出登录吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      })
      userStore.logout()
      ElMessage.success('退出登录成功')
      router.push('/login')
    } catch {
      // cancelled
    }
  }
}

onMounted(() => {
  loadSessions()
})
</script>

<style scoped>
.ai-chat-container {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 120px);
  margin: -20px;
  background-color: #f5f7fa;
}

.chat-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 24px;
  background: white;
  border-bottom: 1px solid #e4e7ed;
  flex-shrink: 0;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.header-left h2 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #303133;
}

.header-center {
  display: flex;
  gap: 4px;
}

.header-right {
  cursor: pointer;
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 20px 24px;
  scroll-behavior: smooth;
}

.welcome-message {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
}

.welcome-content {
  text-align: center;
  color: #606266;
}

.welcome-icon {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 20px;
}

.welcome-icon .el-icon {
  color: white !important;
}

.welcome-content h3 {
  margin: 0 0 8px;
  color: #303133;
  font-size: 22px;
}

.welcome-desc {
  color: #909399;
  margin-bottom: 32px;
}

.quick-actions {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
  max-width: 480px;
  margin: 0 auto;
}

.quick-action-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  background: white;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
  font-size: 14px;
  color: #606266;
}

.quick-action-item:hover {
  border-color: #409eff;
  color: #409eff;
  background: #ecf5ff;
}

.message-item {
  margin-bottom: 24px;
}

.message-item.user {
  display: flex;
  justify-content: flex-end;
}

.message-item.assistant {
  display: flex;
  justify-content: flex-start;
}

.user-message {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  max-width: 70%;
}

.user-message .message-content {
  background: linear-gradient(135deg, #409eff 0%, #337ecc 100%);
  color: white;
  padding: 10px 16px;
  border-radius: 12px 12px 4px 12px;
  word-wrap: break-word;
  line-height: 1.6;
  font-size: 14px;
}

.assistant-message {
  display: flex;
  gap: 12px;
  max-width: 80%;
}

.message-avatar {
  flex-shrink: 0;
}

.message-text {
  flex: 1;
  min-width: 0;
}

.assistant-message .message-content {
  background-color: white;
  color: #303133;
  padding: 10px 16px;
  border-radius: 12px 12px 12px 4px;
  border: 1px solid #e4e7ed;
  word-wrap: break-word;
  line-height: 1.6;
  font-size: 14px;
}

.assistant-message .message-content :deep(code) {
  background: #f5f7fa;
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 13px;
  color: #e6a23c;
}

.message-meta {
  display: flex;
  gap: 12px;
  margin-top: 4px;
  font-size: 12px;
  color: #c0c4cc;
  padding: 0 4px;
}

.user-message .message-meta {
  justify-content: flex-end;
}

.loading-indicator {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 0 0 0 0;
  margin-bottom: 24px;
}

.loading-dots {
  display: flex;
  gap: 6px;
  padding: 14px 18px;
  background: white;
  border-radius: 12px 12px 12px 4px;
  border: 1px solid #e4e7ed;
}

.loading-dots span {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #c0c4cc;
  animation: bounce 1.4s infinite both;
}

.loading-dots span:nth-child(1) { animation-delay: 0s; }
.loading-dots span:nth-child(2) { animation-delay: 0.2s; }
.loading-dots span:nth-child(3) { animation-delay: 0.4s; }

@keyframes bounce {
  0%, 80%, 100% { transform: scale(0.6); opacity: 0.4; }
  40% { transform: scale(1); opacity: 1; }
}

.chat-input-area {
  background: white;
  border-top: 1px solid #e4e7ed;
  padding: 16px 24px;
  flex-shrink: 0;
}

.input-container {
  display: flex;
  gap: 12px;
  align-items: flex-end;
  max-width: 900px;
  margin: 0 auto;
}

.input-container :deep(.el-textarea__inner) {
  border-radius: 8px;
  padding: 10px 16px;
}

.send-btn {
  flex-shrink: 0;
  border-radius: 8px;
  min-width: 80px;
}

.history-content {
  padding: 16px;
}

.history-item {
  padding: 12px 16px;
  margin-bottom: 8px;
  background: #f5f7fa;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
  border: 2px solid transparent;
}

.history-item:hover {
  background: #ecf5ff;
  border-color: #b3d8ff;
}

.history-item.active {
  background: #ecf5ff;
  border-color: #409eff;
}

.session-title {
  font-weight: 600;
  color: #303133;
  margin-bottom: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.session-meta {
  display: flex;
  gap: 12px;
  font-size: 12px;
  color: #909399;
}

@media (max-width: 768px) {
  .ai-chat-container {
    margin: -20px -20px;
    height: calc(100vh - 100px);
  }

  .chat-header {
    padding: 10px 16px;
  }

  .header-center {
    display: none;
  }

  .chat-messages {
    padding: 12px;
  }

  .chat-input-area {
    padding: 12px 16px;
  }

  .user-message,
  .assistant-message {
    max-width: 90%;
  }

  .quick-actions {
    grid-template-columns: 1fr;
  }
}
</style>
