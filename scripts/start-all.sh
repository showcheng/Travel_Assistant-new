#!/bin/bash

# 智慧旅游助手平台 - 服务启动脚本

echo "========================================"
echo "  智慧旅游助手平台 - 服务启动脚本"
echo "========================================"
echo ""

# 检查 Java 环境
if ! command -v java &> /dev/null; then
    echo "❌ 错误: 未找到 Java 环境"
    echo "请先安装 JDK 17 或更高版本"
    exit 1
fi

JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d'.' -f1)
if [ "$JAVA_VERSION" -lt 17 ]; then
    echo "❌ 错误: Java 版本过低 (当前: $JAVA_VERSION, 需要: 17+)"
    exit 1
fi

echo "✅ Java 版本检查通过: $JAVA_VERSION"
echo ""

# 检查 PostgreSQL
if ! command -v psql &> /dev/null; then
    echo "⚠️  警告: 未找到 PostgreSQL，请确保数据库已启动"
else
    echo "✅ PostgreSQL 已安装"
fi

# 检查 Redis
if ! command -v redis-cli &> /dev/null; then
    echo "⚠️  警告: 未找到 Redis，请确保 Redis 已启动"
else
    echo "✅ Redis 已安装"
fi

echo ""
echo "========================================"
echo "  正在启动服务..."
echo "========================================"
echo ""

# 服务列表
services=("travel-common" "travel-user" "travel-product" "travel-order" "travel-seckill" "travel-group" "travel-ai" "travel-live" "travel-gateway")
ports=(8080 8081 8082 8083 8084 8085 8086 8087 8088)

# 启动函数
start_service() {
    local service=$1
    local port=$2

    echo "🚀 启动 $service (端口: $port)..."

    cd "$service" || {
        echo "❌ 错误: 找不到服务目录 $service"
        return 1
    }

    if [ ! -f "pom.xml" ]; then
        echo "⚠️  跳过 $service (未找到 pom.xml)"
        cd ..
        return 0
    fi

    # 后台启动
    nohup mvn spring-boot:run > ../logs/$service.log 2>&1 &
    local pid=$!

    echo "✅ $service 已启动 (PID: $pid)"
    echo "$pid" > ../pids/$service.pid

    cd ..

    # 等待服务启动
    echo "⏳ 等待 $service 启动..."
    sleep 5

    # 检查服务是否启动成功
    if curl -s "http://localhost:$port/actuator/health" > /dev/null 2>&1; then
        echo "✅ $service 启动成功！"
    else
        echo "⚠️  $service 启动可能失败，请查看日志: logs/$service.log"
    fi

    echo ""
}

# 创建必要的目录
mkdir -p logs pids

# 启动所有服务
for i in "${!services[@]}"; do
    service="${services[$i]}"
    port="${ports[$i]}"

    if [ "$service" = "travel-common" ]; then
        continue  # 跳过公共模块
    fi

    start_service "$service" "$port"
done

echo "========================================"
echo "  服务启动完成！"
echo "========================================"
echo ""
echo "📖 API 文档:"
echo "   - 用户服务: http://localhost:8081/doc.html"
echo "   - 商品服务: http://localhost:8082/doc.html"
echo "   - 订单服务: http://localhost:8083/doc.html"
echo ""
echo "📋 日志目录: logs/"
echo "📋 PID 目录: pids/"
echo ""
echo "🛑 停止所有服务:"
echo "   ./scripts/stop-all.sh"
echo ""
