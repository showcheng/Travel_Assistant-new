#!/bin/bash

# AI服务集成测试脚本

echo "=== AI服务集成测试 ==="
echo ""

# 1. 检查MySQL连接
echo "1. 检查MySQL连接..."
mysql_status=$(docker ps --filter "name=mysql" --format "{{.Status}}" 2>/dev/null)
if [ -n "$mysql_status" ]; then
    echo "✓ MySQL运行正常: $mysql_status"
else
    echo "✗ MySQL未运行"
    exit 1
fi

# 2. 检查Redis连接
echo ""
echo "2. 检查Redis连接..."
if redis-cli ping >/dev/null 2>&1; then
    echo "✓ Redis运行正常"
else
    echo "✗ Redis未运行"
    exit 1
fi

# 3. 启动AI服务
echo ""
echo "3. 启动AI服务..."
cd travel-assistant/travel-ai
mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=8086" &
AI_PID=$!
echo "AI服务启动中，PID: $AI_PID"

# 等待服务启动
echo "等待服务启动 (30秒)..."
for i in {1..30}; do
    if curl -s http://localhost:8086/api/ai/health >/dev/null 2>&1; then
        echo "✓ AI服务启动成功"
        break
    fi
    sleep 1
    echo -n "."
done

# 4. 测试健康检查接口
echo ""
echo ""
echo "4. 测试健康检查接口..."
health_response=$(curl -s http://localhost:8086/api/ai/health)
echo "健康检查响应: $health_response"

# 5. 测试对话接口
echo ""
echo "5. 测试对话接口..."
chat_response=$(curl -s -X POST http://localhost:8086/api/ai/chat/send \
  -H "Content-Type: application/json" \
  -d '{"message":"你好"}')
echo "对话响应: $chat_response"

# 6. 清理
echo ""
echo "测试完成，清理进程..."
kill $AI_PID 2>/dev/null

echo ""
echo "=== 测试完成 ==="
