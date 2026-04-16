#!/bin/bash

# AI服务API测试脚本

BASE_URL="http://localhost:8086"

echo "=========================================="
echo "  AI服务API测试"
echo "=========================================="
echo ""

# 1. 健康检查
echo "1. 健康检查测试"
curl -s "$BASE_URL/api/ai/health" | jq '.'
echo ""
echo ""

# 2. 对话测试
echo "2. 对话功能测试"
echo "发送消息: 你好"
curl -s -X POST "$BASE_URL/api/ai/chat/send" \
  -H "Content-Type: application/json" \
  -d '{
    "message": "你好，请介绍一下自己"
  }' | jq '.'
echo ""
echo ""

# 3. 带sessionId的对话测试
echo "3. 带会话ID的对话测试"
SESSION_ID="test-session-$(date +%s)"
echo "会话ID: $SESSION_ID"
curl -s -X POST "$BASE_URL/api/ai/chat/send" \
  -H "Content-Type: application/json" \
  -d "{
    \"message\": \"北京有哪些著名景点？\",
    \"sessionId\": \"$SESSION_ID\"
  }" | jq '.'
echo ""
echo ""

# 4. 第二轮对话（上下文测试）
echo "4. 上下文对话测试"
curl -s -X POST "$BASE_URL/api/ai/chat/send" \
  -H "Content-Type: application/json" \
  -d "{
    \"message\": \"这些景点中哪个最适合老年人？\",
    \"sessionId\": \"$SESSION_ID\"
  }" | jq '.'
echo ""
echo ""

echo "=========================================="
echo "  测试完成"
echo "=========================================="
