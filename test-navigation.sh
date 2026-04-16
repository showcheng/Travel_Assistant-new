#!/bin/bash

# 智慧旅游助手 - 导航功能测试脚本

echo "=================================================="
echo "  导航菜单功能测试"
echo "  Navigation Menu Test"
echo "=================================================="
echo ""

# 颜色定义
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo -e "${BLUE}测试准备...${NC}"
echo ""

# 检查前端服务
echo -e "${YELLOW}[1/4]${NC} 检查前端服务..."
if netstat -ano | findstr ":3000" | findstr "LISTENING" > /dev/null; then
    echo -e "${GREEN}✓${NC} 前端服务运行正常 (端口3000)"
else
    echo -e "${RED}✗${NC} 前端服务未运行"
    echo "请先启动前端服务: cd travel-assistant-web && npm run dev"
    exit 1
fi
echo ""

# 检查路由配置
echo -e "${YELLOW}[2/4]${NC} 检查路由配置..."
if grep -q "route=\"/orders\"" travel-assistant-web/src/views/LayoutView.vue; then
    echo -e "${GREEN}✓${NC} 导航菜单配置正确"
else
    echo -e "${RED}✗${NC} 导航菜单配置有问题"
fi
echo ""

# 检查页面文件
echo -e "${YELLOW}[3/4]${NC} 检查页面文件..."
PAGES=("OrderListView" "OrderStatisticsView" "ProfileView")
for page in "${PAGES[@]}"; do
    if [ -f "travel-assistant-web/src/views/${page}.vue" ]; then
        echo -e "${GREEN}✓${NC} ${page}.vue 存在"
    else
        echo -e "${RED}✗${NC} ${page}.vue 不存在"
    fi
done
echo ""

# API测试
echo -e "${YELLOW}[4/4]${NC} 测试API连接..."
echo "测试用户服务代理..."
USER_TEST=$(curl -s -X POST http://localhost:3000/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"phone":"13800138000","password":"123456"}')

if echo "$USER_TEST" | grep -q "accessToken"; then
    echo -e "${GREEN}✓${NC} API代理正常工作"
    echo "   JWT Token: 已生成"
else
    echo -e "${RED}✗${NC} API代理测试失败"
fi
echo ""

echo "=================================================="
echo -e "${GREEN}  导航功能测试完成${NC}"
echo "=================================================="
echo ""
echo -e "${BLUE}修复的导航功能:${NC}"
echo "  ✅ 我的订单页面 - 点击顶部菜单'我的订单'"
echo "  ✅ 订单统计页面 - 点击顶部菜单'订单统计'"
echo "  ✅ 个人中心页面 - 点击顶部菜单'个人中心'"
echo ""
echo -e "${BLUE}测试步骤:${NC}"
echo "  1. 访问: http://localhost:3000/login"
echo "  2. 登录: 13800138000 / 123456"
echo "  3. 登录后会自动跳转到商品页面"
echo "  4. 点击顶部导航栏的菜单项进行测试"
echo ""
echo -e "${BLUE}预期结果:${NC}"
echo "  • 点击菜单项后页面立即跳转"
echo "  • URL地址正确更新"
echo "  • 页面内容正常显示"
echo "  • 菜单项高亮显示"
echo ""
echo -e "${YELLOW}💡 提示:${NC}"
echo "  • 如果菜单还是无法点击，请清除浏览器缓存"
echo "  • 按 Ctrl+Shift+Delete 清除缓存"
echo "  • 或按 Ctrl+F5 强制刷新页面"
echo "  • 确保已完成登录操作"
echo ""
echo -e "${GREEN}🚀 开始测试导航功能！${NC}"
echo "=================================================="