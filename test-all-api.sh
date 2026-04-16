#!/bin/bash

# 智慧旅游助手 - 自动化API测试脚本
# 测试所有后端API接口

BASE_URL="http://localhost:8088"
TEST_USER_PHONE="13800138000"
TEST_USER_PASSWORD="123456"
TOKEN=""
TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 测试结果数组
declare -a TEST_RESULTS

# 打印标题
print_header() {
    echo -e "${BLUE}========================================${NC}"
    echo -e "${BLUE}$1${NC}"
    echo -e "${BLUE}========================================${NC}"
}

# 打印测试结果
print_test_result() {
    local test_name=$1
    local result=$2
    local message=$3

    TOTAL_TESTS=$((TOTAL_TESTS + 1))

    if [ $result -eq 0 ]; then
        echo -e "${GREEN}✓${NC} $test_name"
        PASSED_TESTS=$((PASSED_TESTS + 1))
        TEST_RESULTS+=("✓ $test_name")
    else
        echo -e "${RED}✗${NC} $test_name: $message"
        FAILED_TESTS=$((FAILED_TESTS + 1))
        TEST_RESULTS+=("✗ $test_name: $message")
    fi
}

# 测试API接口
test_api() {
    local test_name=$1
    local method=$2
    local endpoint=$3
    local data=$4
    local expected_code=$5
    local auth=$6

    local url="${BASE_URL}${endpoint}"
    local auth_header=""
    local response=""
    local http_code=""

    if [ "$auth" = "true" ] && [ -n "$TOKEN" ]; then
        auth_header="-H \"Authorization: Bearer $TOKEN\""
    fi

    if [ "$method" = "GET" ]; then
        response=$(curl -s -w "\n%{http_code}" -X GET "$url" -H "Content-Type: application/json" $auth_header)
    elif [ "$method" = "POST" ]; then
        response=$(curl -s -w "\n%{http_code}" -X POST "$url" -H "Content-Type: application/json" $auth_header -d "$data")
    elif [ "$method" = "PUT" ]; then
        response=$(curl -s -w "\n%{http_code}" -X PUT "$url" -H "Content-Type: application/json" $auth_header -d "$data")
    fi

    http_code=$(echo "$response" | tail -n1)
    body=$(echo "$response" | sed '$d')

    if [ "$http_code" = "$expected_code" ]; then
        print_test_result "$test_name" 0
        return 0
    else
        print_test_result "$test_name" 1 "HTTP $http_code, Response: $body"
        return 1
    fi
}

echo -e "${YELLOW}"
echo "======================================================"
echo "  智慧旅游助手 - 自动化API测试"
echo "  智慧旅游助手 API Automation Testing"
echo "======================================================"
echo -e "${NC}"

# 1. 用户认证测试
print_header "1. 用户认证模块测试"

echo -e "${BLUE}测试用户登录...${NC}"
login_response=$(curl -s -X POST "${BASE_URL}/api/auth/login" \
    -H "Content-Type: application/json" \
    -d "{\"phone\":\"${TEST_USER_PHONE}\",\"password\":\"${TEST_USER_PASSWORD}\"}")

if echo "$login_response" | grep -q "accessToken"; then
    TOKEN=$(echo "$login_response" | grep -o '"accessToken":"[^"]*"' | cut -d'"' -f4)
    print_test_result "用户登录" 0
    echo -e "${GREEN}获取到Token: ${TOKEN:0:20}...${NC}"
else
    print_test_result "用户登录" 1 "登录失败"
    echo -e "${RED}登录响应: $login_response${NC}"
    echo -e "${RED}请确保用户服务正在运行 (端口8081)${NC}"
    exit 1
fi

# 2. 商品服务测试
print_header "2. 商品服务模块测试"

test_api "获取商品列表" "GET" "/api/products" "" "200" "false"
test_api "搜索商品" "GET" "/api/products?page=1&size=5" "" "200" "false"

# 3. 订单服务测试
print_header "3. 订单服务模块测试"

test_api "获取用户订单" "GET" "/api/orders" "" "200" "true"
test_api "获取订单统计" "GET" "/api/orders/statistics" "" "200" "true"

# 4. 用户信息测试
print_header "4. 用户信息模块测试"

test_api "获取当前用户信息" "GET" "/api/users/current" "" "200" "true"

# 5. 订单搜索测试
print_header "5. 订单搜索功能测试"

test_api "搜索订单" "GET" "/api/orders/search?keyword=ORDER" "" "200" "true"

# 测试完成，输出总结
print_header "测试结果总结"

echo -e "总测试数: ${BLUE}$TOTAL_TESTS${NC}"
echo -e "${GREEN}通过: $PASSED_TESTS${NC}"
echo -e "${RED}失败: $FAILED_TESTS${NC}"

if [ $FAILED_TESTS -eq 0 ]; then
    echo -e "\n${GREEN}🎉 所有测试通过！${NC}\n"
    exit 0
else
    echo -e "\n${RED}⚠️  部分测试失败，请检查服务状态${NC}\n"

    echo -e "${YELLOW}失败详情:${NC}"
    for result in "${TEST_RESULTS[@]}"; do
        if [[ $result == ✗* ]]; then
            echo -e "${RED}$result${NC}"
        fi
    done

    exit 1
fi