#!/bin/bash

# GLM-5 API连接测试脚本

echo "=========================================="
echo "  GLM-5 API连接测试"
echo "=========================================="
echo ""

# API密钥
API_KEY="e217140203d34544acb721230c4f3d57.M4VERrYX2rKgB66r"
API_URL="https://open.bigmodel.cn/api/paas/v4/chat/completions"

echo "1. 测试API密钥格式..."
if [[ $API_KEY == *.M4VERrYX2rKgB66r ]]; then
    echo "✅ API密钥格式正确"
else
    echo "❌ API密钥格式不正确"
    exit 1
fi

echo ""
echo "2. 测试GLM-4 API连接..."

# 发送测试请求
response=$(curl -s -X POST "$API_URL" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $API_KEY" \
  -d '{
    "model": "glm-4",
    "messages": [
      {
        "role": "user",
        "content": "你好，请用一句话介绍你自己"
      }
    ],
    "temperature": 0.7,
    "max_tokens": 100
  }')

echo ""
echo "3. API响应结果:"
echo "$response" | jq '.'

# 检查响应
if echo "$response" | jq -e '.choices[0].message.content' > /dev/null 2>&1; then
    echo ""
    echo "✅ API连接测试成功！"

    # 提取AI回复
    ai_reply=$(echo "$response" | jq -r '.choices[0].message.content')
    echo ""
    echo "AI回复: $ai_reply"

    # 显示token使用情况
    echo ""
    echo "Token使用情况:"
    echo "$response" | jq '.usage'

    exit 0
else
    echo ""
    echo "❌ API连接测试失败"
    echo "错误信息:"
    echo "$response" | jq '.'
    exit 1
fi
