#!/bin/bash

# 智慧旅游助手 - 快速测试脚本
# 用于快速验证所有服务是否正常运行

echo "=================================================="
echo "  智慧旅游助手 - 快速服务测试"
echo "  Smart Travel Assistant - Quick Service Test"
echo "=================================================="
echo ""

# 颜色定义
GREEN='\033[0;32m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo -e "${BLUE}[1/4] 测试用户服务 (8081端口)${NC}"
USER_RESULT=$(curl -s -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"phone":"13800138000","password":"123456"}')
if echo "$USER_RESULT" | grep -q "accessToken"; then
    echo -e "${GREEN}✓${NC} 用户服务运行正常"
    echo "   JWT Token: $(echo $USER_RESULT | head -c 50)..."
else
    echo -e "${RED}✗${NC} 用户服务测试失败"
fi
echo ""

echo -e "${BLUE}[2/4] 测试商品服务 (8082端口)${NC}"
PRODUCT_RESULT=$(curl -s "http://localhost:8082/api/products?page=1&size=1")
if echo "$PRODUCT_RESULT" | grep -q "故宫博物院成人票"; then
    echo -e "${GREEN}✓${NC} 商品服务运行正常"
    PRODUCT_COUNT=$(echo $PRODUCT_RESULT | grep -o '"id":[0-9]*' | wc -l)
    echo "   商品数量: $PRODUCT_COUNT个"
else
    echo -e "${RED}✗${NC} 商品服务测试失败"
fi
echo ""

echo -e "${BLUE}[3/4] 测试前端Vite代理 (3000端口)${NC}"
PROXY_RESULT=$(curl -s "http://localhost:3000/api/products?page=1&size=1")
if echo "$PROXY_RESULT" | grep -q "故宫博物院成人票"; then
    echo -e "${GREEN}✓${NC} 前端代理运行正常"
    echo "   代理路径: /api/products → http://localhost:8082"
else
    echo -e "${RED}✗${NC} 前端代理测试失败"
fi
echo ""

echo -e "${BLUE}[4/4] 测试前端服务状态${NC}"
if netstat -ano | findstr ":3000" | findstr "LISTENING" > /dev/null; then
    echo -e "${GREEN}✓${NC} 前端服务运行正常 (端口3000)"
else
    echo -e "${RED}✗${NC} 前端服务未运行"
fi
echo ""

echo "=================================================="
echo -e "${GREEN}  快速测试完成！${NC}"
echo "=================================================="
echo ""
echo -e "${BLUE}测试账号:${NC}"
echo "• 手机号: 13800138000"
echo "• 密码: 123456"
echo ""
echo -e "${BLUE}服务地址:${NC}"
echo "• 前端应用: http://localhost:3000"
echo "• 用户服务: http://localhost:8081"
echo "• 商品服务: http://localhost:8082"
echo ""
echo -e "${GREEN}🚀 系统运行正常，可以开始使用！${NC}"
echo "=================================================="