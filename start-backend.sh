#!/bin/bash

# 智慧旅游助手 - 微服务启动脚本

echo "======================================================"
echo "  智慧旅游助手 - 微服务启动"
echo "  Microservices Startup"
echo "======================================================"
echo ""

# 颜色定义
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# 服务端口配置
USER_PORT=8081
PRODUCT_PORT=8082
ORDER_PORT=8083
GATEWAY_PORT=8088

# 服务目录
USER_DIR="travel-user/target"
PRODUCT_DIR="travel-product/target"
ORDER_DIR="travel-order/target"
GATEWAY_DIR="travel-gateway/target"

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

# 启动服务
start_service() {
    local service_dir=$1
    local service_name=$2
    local service_port=$3
    local jar_file=$4

    echo -e "${BLUE}启动 $service_name...${NC}"

    if [ ! -f "$service_dir/$jar_file" ]; then
        echo -e "${RED}✗${NC} 找不到jar文件: $service_dir/$jar_file"
        return 1
    fi

    cd "$service_dir"
    nohup java -jar "$jar_file" > "logs/$service_name.log" 2>&1 &
    local pid=$!

    echo -e "${GREEN}✓${NC} $service_name 已启动 (PID: $pid, Port: $service_port)"
    cd ../..

    # 等待服务启动
    echo -e "${YELLOW}等待 $service_name 启动...${NC}"
    sleep 5

    return 0
}

# 检查依赖
check_dependencies() {
    echo -e "${BLUE}[1/5]${NC} 检查Java环境..."
    if command -v java &> /dev/null; then
        JAVA_VERSION=$(java -version 2>&1 | head -n 1)
        echo -e "${GREEN}✓${NC} Java环境正常: $JAVA_VERSION"
    else
        echo -e "${RED}✗${NC} Java未安装，请先安装Java 17+"
        exit 1
    fi
}

# 启动服务
echo -e "${BLUE}[2/5]${NC} 检查端口占用..."
echo ""

# 检查并启动用户服务
if check_port $USER_PORT "用户服务"; then
    echo -e "${YELLOW}尝试启动用户服务...${NC}"
    if [ -f "$USER_DIR/travel-user-1.0.0.jar" ]; then
        start_service "$USER_DIR" "用户服务" $USER_PORT "travel-user-1.0.0.jar"
    else
        echo -e "${RED}用户服务jar文件不存在，需要先编译${NC}"
    fi
fi
echo ""

# 检查并启动商品服务
if check_port $PRODUCT_PORT "商品服务"; then
    echo -e "${YELLOW}尝试启动商品服务...${NC}"
    if [ -f "$PRODUCT_DIR/travel-product-1.0.0.jar" ]; then
        start_service "$PRODUCT_DIR" "商品服务" $PRODUCT_PORT "travel-product-1.0.0.jar"
    else
        echo -e "${RED}商品服务jar文件不存在，需要先编译${NC}"
    fi
fi
echo ""

# 跳过订单服务（编译问题）
echo -e "${YELLOW}[3/5]${NC} 订单服务暂时跳过（编译问题）"
echo ""

# 检查网关服务
if ! check_port $GATEWAY_PORT "网关服务"; then
    echo -e "${YELLOW}尝试启动网关服务...${NC}"
    if [ -f "$GATEWAY_DIR/travel-gateway-1.0.0.jar" ]; then
        start_service "$GATEWAY_DIR" "网关服务" $GATEWAY_PORT "travel-gateway-1.0.0.jar"
    else
        echo -e "${RED}网关服务jar文件不存在，需要先编译${NC}"
    fi
fi
echo ""

echo -e "${BLUE}[4/5]${NC} 等待所有服务完全启动..."
sleep 10
echo ""

# 检查服务状态
echo -e "${BLUE}[5/5]${NC} 检查服务状态..."
echo ""

all_services_ok=true

# 检查用户服务
if netstat -ano | findstr ":$USER_PORT" | findstr "LISTENING" > /dev/null; then
    echo -e "${GREEN}✓${NC} 用户服务运行正常 (端口: $USER_PORT)"
else
    echo -e "${RED}✗${NC} 用户服务未启动"
    all_services_ok=false
fi

# 检查商品服务
if netstat -ano | findstr ":$PRODUCT_PORT" | findstr "LISTENING" > /dev/null; then
    echo -e "${GREEN}✓${NC} 商品服务运行正常 (端口: $PRODUCT_PORT)"
else
    echo -e "${RED}✗${NC} 商品服务未启动"
    all_services_ok=false
fi

# 检查网关服务
if netstat -ano | findstr ":$GATEWAY_PORT" | findstr "LISTENING" > /dev/null; then
    echo -e "${GREEN}✓${NC} 网关服务运行正常 (端口: $GATEWAY_PORT)"
else
    echo -e "${RED}✗${NC} 网关服务未启动"
    all_services_ok=false
fi

echo ""
echo "======================================================"

if [ "$all_services_ok" = true ]; then
    echo -e "${GREEN}  🎉 所有服务启动成功！${NC}"
else
    echo -e "${YELLOW}  ⚠️  部分服务启动失败${NC}"
fi

echo "======================================================"
echo ""
echo -e "${BLUE}服务地址:${NC}"
echo "• 用户服务: http://localhost:$USER_PORT"
echo "• 商品服务: http://localhost:$PRODUCT_PORT"
echo "• 网关服务: http://localhost:$GATEWAY_PORT"
echo ""
echo -e "${BLUE}测试账号:${NC}"
echo "• 手机号: 13800138000"
echo "• 密码: 123456"
echo ""
echo -e "${YELLOW}💡 提示:${NC}"
echo "• 前端应用: http://localhost:3000"
echo "• 可以开始前后端联调测试"
echo "• 订单服务暂时跳过（编译问题）"
echo "======================================================"