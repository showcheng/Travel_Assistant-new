#!/bin/bash

# GLM-5 API 连接测试脚本 (修复版)

API_KEY="${ZHIPUAI_API_KEY}"
API_URL="https://open.bigmodel.cn/api/paas/v4/chat/completions"

echo "🔍 测试 GLM-5 API 连接..."
echo ""

# 检查 API 密钥
if [ -z "$API_KEY" ]; then
    echo "❌ ZHIPUAI_API_KEY 环境变量未设置"
    echo "   请先设置: export ZHIPUAI_API_KEY='your-api-key'"
    exit 1
fi

echo "✅ API 密钥: ${API_KEY:0:20}..."
echo ""

# 创建临时的 JSON 文件以避免编码问题
cat > /tmp/glm_test_request.json << 'EOF'
{
  "model": "glm-4-flash",
  "messages": [
    {
      "role": "user",
      "content": "Hello"
    }
  ],
  "temperature": 0.7,
  "max_tokens": 50
}
EOF

echo "📡 发送测试请求到智谱 AI..."
echo ""

if command -v curl &> /dev/null; then
    RESPONSE=$(curl -s -X POST "$API_URL" \
        -H "Content-Type: application/json; charset=utf-8" \
        -H "Authorization: Bearer $API_KEY" \
        --data-binary @/tmp/glm_test_request.json 2>&1)

    # 清理临时文件
    rm -f /tmp/glm_test_request.json

    # 检查响应
    if echo "$RESPONSE" | grep -q "choices"; then
        echo "✅ API 连接成功！"
        echo ""
        echo "📝 响应摘要:"
        echo "$RESPONSE" | python3 -m json.tool 2>/dev/null | grep -A 2 '"content"' | head -3 || echo "$RESPONSE" | head -10
        echo ""
        echo "🎉 GLM-5 API 配置完成并工作正常！"
    elif echo "$RESPONSE" | grep -q "error\|error_code"; then
        echo "⚠️ API 返回错误:"
        echo "$RESPONSE" | python3 -c "import sys, json; data=json.load(sys.stdin); print('错误:', data.get('error', {}).get('message', '未知错误'))" 2>/dev/null || echo "$RESPONSE"
        echo ""
        echo "💡 可能的原因:"
        echo "   - API 密钥格式不正确"
        echo "   - 账户余额不足"
        echo "   - API 端点或模型名称变更"
    else
        echo "⚠️ 未知响应:"
        echo "$RESPONSE" | head -10
    fi
else
    echo "❌ curl 命令不可用，无法测试 API 连接"
    echo "   请安装 curl 或手动测试 API"
fi

echo ""
echo "📋 配置摘要:"
echo "   - API 端点: $API_URL"
echo "   - 测试模型: glm-4-flash"
echo "   - 生产模型: GLM-5, GLM-5-Turbo, GLM-5-Plus"
