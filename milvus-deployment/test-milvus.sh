#!/bin/bash

# Milvus连接测试脚本

echo "=========================================="
echo "  Milvus连接测试"
echo "=========================================="
echo ""

# 检查容器状态
echo "1. 检查容器状态..."
echo ""

podman ps --filter "pod=milvus-pod" --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"

echo ""

# 检查Milvus日志
echo "2. 检查Milvus启动日志..."
echo ""

podman logs --tail 20 milvus-standalone 2>&1 | grep -i "started\|listening\|ready\|error" || echo "等待Milvus启动..."

echo ""

# 测试Milvus端口连接
echo "3. 测试Milvus端口连接..."
echo ""

# 检查端口19530是否开放
if command -v nc &> /dev/null; then
    if nc -zv localhost 19530 2>&1 | grep -q "succeeded"; then
        echo "✅ Milvus端口19530可访问"
    else
        echo "⚠️  Milvus端口19530暂时不可访问"
    fi
elif command -v timeout &> /dev/null; then
    if timeout 2 bash -c "cat < /dev/null > /dev/tcp/localhost/19530" 2>/dev/null; then
        echo "✅ Milvus端口19530可访问"
    else
        echo "⚠️  Milvus端口19530暂时不可访问"
    fi
else
    echo "无法测试端口连接（缺少nc或timeout命令）"
fi

echo ""

# 检查容器健康状态
echo "4. 容器健康状态..."
echo ""

for container in milvus-etcd milvus-minio milvus-standalone; do
    if podman ps -q -f name=$container | grep -q .; then
        echo "✅ $container 运行中"
    else
        echo "❌ $container 未运行"
    fi
done

echo ""

echo "=========================================="
echo "  测试完成"
echo "=========================================="
echo ""
echo "如需查看详细日志:"
echo "  podman logs -f milvus-standalone"
echo "  podman logs milvus-etcd"
echo "  podman logs milvus-minio"
echo ""
