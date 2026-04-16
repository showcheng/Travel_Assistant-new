#!/bin/bash

# 智慧旅游助手 - 网关服务启动脚本

echo "=================================================="
echo "  网关服务启动 - Gateway Service Startup"
echo "=================================================="
echo ""

# 颜色定义
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# 检查端口占用
check_port() {
    local port=$1
    local service_name=$2

    if netstat -ano | findstr ":$port" | findstr "LISTENING" > /dev/null; then
        echo -e "${YELLOW}⚠️${NC} $service_name已在端口 $port 运行"
        return 1
    else
        return 0
    fi
}

# 启动网关服务
start_gateway() {
    echo -e "${BLUE}[1/2]${NC} 检查网关服务编译状态..."

    # 检查jar文件是否存在
    if [ ! -f "travel-assistant/travel-gateway/target/travel-gateway-1.0.0.jar" ]; then
        echo -e "${RED}✗${NC} 网关服务jar文件不存在，开始编译..."
        cd travel-assistant/travel-gateway
        mvn clean package -DskipTests
        if [ $? -ne 0 ]; then
            echo -e "${RED}✗${NC} 编译失败，请检查错误信息"
            return 1
        fi
        cd ../..
    fi

    echo -e "${GREEN}✓${NC} 编译完成"

    echo ""
    echo -e "${BLUE}[2/2]${NC} 启动网关服务..."

    cd travel-assistant/travel-gateway/target

    # 启动网关服务
    nohup java -jar travel-gateway-1.0.0.jar > gateway.log 2>&1 &
    GATEWAY_PID=$!

    echo -e "${GREEN}✓${NC} 网关服务已启动 (PID: $GATEWAY_PID, Port: 8088)"
    cd ../../..

    # 等待服务启动
    echo ""
    echo -e "${YELLOW}等待网关服务启动...${NC}"
    sleep 10

    # 检查服务状态
    if netstat -ano | findstr ":8088" | findstr "LISTENING" > /dev/null; then
        echo -e "${GREEN}✓${NC} 网关服务启动成功！"

        # 测试CORS配置
        echo ""
        echo -e "${BLUE}测试CORS配置...${NC}"
        CORS_TEST=$(curl -s -X OPTIONS "http://localhost:8088/api/auth/login" \
            -H "Origin: http://localhost:3000" \
            -H "Access-Control-Request-Method: POST" \
            -w "HTTP_CODE:%{http_code}")

        if echo "$CORS_TEST" | grep -q "HTTP_CODE:200"; then
            echo -e "${GREEN}✓${NC} CORS配置正常"
        else
            echo -e "${YELLOW}⚠️${NC} CORS配置可能需要调整"
        fi
    else
        echo -e "${RED}✗${NC} 网关服务启动失败，请检查日志:"
        echo "  tail -f travel-assistant/travel-gateway/target/gateway.log"
        return 1
    fi

    echo ""
    echo "=================================================="
    echo -e "${GREEN}  🎉 网关服务启动成功！${NC}"
    echo "=================================================="
    echo ""
    echo -e "${BLUE}服务信息:${NC}"
    echo "• 网关地址: http://localhost:8088"
    echo "• 服务PID: $GATEWAY_PID"
    echo "• 日志文件: travel-assistant/travel-gateway/target/gateway.log"
    echo ""
    echo -e "${BLUE}路由配置:${NC}"
    echo "• 用户服务: /api/auth/**, /api/user/** → 8081"
    echo "• 商品服务: /api/products/** → 8082"
    echo "• 订单服务: /api/orders/**, /api/order-items/** → 8083"
    echo ""
    echo -e "${YELLOW}💡 提示:${NC}"
    echo "• 如果出现403错误，请检查CORS配置"
    echo "• 推荐使用Vite代理方案: http://localhost:3000"
    echo "• 查看详细指南: CORS_FIX_GUIDE.md"
    echo "=================================================="

    return 0
}

# 检查端口
if check_port 8088 "网关服务"; then
    start_gateway
else
    echo ""
    echo "=================================================="
    echo -e "${YELLOW}  ⚠️  网关服务已在运行${NC}"
    echo "=================================================="
    echo ""
    echo -e "${BLUE}当前网关服务:${NC}"
    local GATEWAY_PID=$(netstat -ano | findstr ":8088" | findstr "LISTENING" | head -1 | awk '{print $5}')
    echo "• PID: $GATEWAY_PID"
    echo "• 端口: 8088"
    echo ""
    echo -e "${YELLOW}💡 提示:${NC}"
    echo "• 如需重启网关，请先停止现有服务: taskkill /F /PID $GATEWAY_PID"
    echo "• 或使用: ./start-gateway.sh force"
    echo "=================================================="
fi