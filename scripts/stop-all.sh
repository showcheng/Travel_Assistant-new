#!/bin/bash

# 智慧旅游助手平台 - 服务停止脚本

echo "========================================"
echo "  智慧旅游助手平台 - 服务停止脚本"
echo "========================================"
echo ""

# 检查 PID 目录
if [ ! -d "pids" ]; then
    echo "❌ 错误: 未找到 PID 目录"
    echo "请先使用 start-all.sh 启动服务"
    exit 1
fi

# 服务列表
services=("travel-user" "travel-product" "travel-order" "travel-seckill" "travel-group" "travel-ai" "travel-live" "travel-gateway")

# 停止函数
stop_service() {
    local service=$1
    local pid_file="pids/$service.pid"

    if [ ! -f "$pid_file" ]; then
        echo "⚠️  未找到 $service 的 PID 文件"
        return 0
    fi

    local pid=$(cat "$pid_file")

    if ! kill -0 "$pid" 2>/dev/null; then
        echo "⚠️  $service 未运行 (PID: $pid)"
        rm -f "$pid_file"
        return 0
    fi

    echo "🛑 停止 $service (PID: $pid)..."
    kill "$pid"

    # 等待进程结束
    local count=0
    while kill -0 "$pid" 2>/dev/null; do
        if [ $count -ge 30 ]; then
            echo "⚠️  $service 未能在 30 秒内停止，强制终止..."
            kill -9 "$pid"
            break
        fi
        sleep 1
        count=$((count + 1))
    done

    rm -f "$pid_file"
    echo "✅ $service 已停止"
}

# 停止所有服务
for service in "${services[@]}"; do
    stop_service "$service"
done

echo ""
echo "========================================"
echo "  所有服务已停止"
echo "========================================"
echo ""
