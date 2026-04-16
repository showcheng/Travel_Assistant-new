#!/bin/bash

# 智慧旅游助手 - 一键自动化测试启动脚本

echo "=================================================="
echo "  智慧旅游助手 - 自动化测试启动"
echo "  智慧旅游助手 - Automated Testing"
echo "=================================================="
echo ""

# 颜色定义
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${BLUE}正在启动自动化测试环境...${NC}"
echo ""

# 检查Node.js环境
echo -e "${YELLOW}[1/4]${NC} 检查Node.js环境..."
if command -v node &> /dev/null; then
    NODE_VERSION=$(node --version)
    echo -e "${GREEN}✓${NC} Node.js已安装: $NODE_VERSION"
else
    echo -e "${GREEN}✓${NC} Node.js未安装，将使用浏览器手动测试"
fi

# 检查前端服务器状态
echo -e "${YELLOW}[2/4]${NC} 检查前端服务器状态..."
if netstat -ano | findstr :3000 | findstr LISTENING > /dev/null; then
    echo -e "${GREEN}✓${NC} 前端服务器正在运行 (端口3000)"
else
    echo -e "${YELLOW}⚠️${NC} 前端服务器未启动"
    echo -e "${BLUE}正在启动前端服务器...${NC}"

    cd travel-assistant-web

    # 在后台启动前端服务器
    if command -v npm &> /dev/null; then
        npm run dev &
        FRONTEND_PID=$!
        echo -e "${GREEN}✓${NC} 前端服务器已启动 (PID: $FRONTEND_PID)"
        echo -e "${BLUE}前端地址: http://localhost:3000${NC}"

        # 等待服务器启动
        echo -e "${YELLOW}等待服务器启动...${NC}"
        sleep 5
    else
        echo -e "${RED}✗${NC} npm未安装，请手动启动前端服务器"
    fi

    cd ..
fi

# 打开测试检查清单
echo -e "${YELLOW}[3/4]${NC} 打开测试检查清单..."
TEST_CHECKLIST="file:///d:/JAVA_Porject/Travel_Assistant-new/test-checklist.html"

if [[ "$OSTYPE" == "msys" || "$OSTYPE" == "win32" ]]; then
    # Windows系统
    start "" "$TEST_CHECKLIST"
    echo -e "${GREEN}✓${NC} 测试检查清单已在浏览器中打开"
elif [[ "$OSTYPE" == "darwin"* ]]; then
    # macOS系统
    open "$TEST_CHECKLIST"
    echo -e "${GREEN}✓${NC} 测试检查清单已在浏览器中打开"
else
    # Linux系统
    xdg-open "$TEST_CHECKLIST" 2>/dev/null || sensible-browser "$TEST_CHECKLIST" 2>/dev/null || firefox "$TEST_CHECKLIST" 2>/dev/null
    echo -e "${GREEN}✓${NC} 测试检查清单已在浏览器中打开"
fi

# 打开前端应用
echo -e "${YELLOW}[4/4]${NC} 打开前端应用..."
FRONTEND_URL="http://localhost:3000"

if [[ "$OSTYPE" == "msys" || "$OSTYPE" == "win32" ]]; then
    # Windows系统
    start "" "$FRONTEND_URL"
    echo -e "${GREEN}✓${NC} 前端应用已在浏览器中打开"
elif [[ "$OSTYPE" == "darwin"* ]]; then
    # macOS系统
    open "$FRONTEND_URL"
    echo -e "${GREEN}✓${NC} 前端应用已在浏览器中打开"
else
    # Linux系统
    xdg-open "$FRONTEND_URL" 2>/dev/null || sensible-browser "$FRONTEND_URL" 2>/dev/null || firefox "$FRONTEND_URL" 2>/dev/null
    echo -e "${GREEN}✓${NC} 前端应用已在浏览器中打开"
fi

echo ""
echo "=================================================="
echo -e "${GREEN}  🎉 自动化测试环境已启动！${NC}"
echo "=================================================="
echo ""
echo -e "${BLUE}测试资源:${NC}"
echo -e "• 测试检查清单: ${GREEN}已自动打开${NC}"
echo -e "• 前端应用: ${GREEN}http://localhost:3000${NC}"
echo -e "• 测试账号: ${GREEN}13800138000 / 123456${NC}"
echo ""
echo -e "${BLUE}测试步骤:${NC}"
echo "1. 在测试检查清单中逐项完成测试"
echo "2. 在前端应用中验证功能正常"
echo "3. 记录发现的问题和异常"
echo "4. 根据需要修复和完善功能"
echo ""
echo -e "${BLUE}详细指南:${NC}"
echo "• 查看 TESTING_GUIDE.md 了解详细测试流程"
echo "• 查看 AUTOMATED_TEST_REPORT.md 了解测试报告格式"
echo "• 运行 test-all-api.sh 进行后端API测试"
echo ""
echo -e "${YELLOW}💡 提示:${NC}"
echo "• 后端服务需要手动启动才能测试完整功能"
echo "• 可以先测试前端界面，再启动后端服务"
echo "• 检查清单会自动保存测试进度"
echo ""
echo -e "${GREEN}🚀 开始测试吧！${NC}"
echo "=================================================="