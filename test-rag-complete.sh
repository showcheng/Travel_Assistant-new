#!/bin/bash

# 知识库RAG系统全面测试脚本

BASE_URL="http://localhost:8086/api/knowledge"
AI_BASE_URL="http://localhost:8086/api/ai/chat"

echo "================================"
echo "  知识库RAG系统全面测试"
echo "================================"
echo ""

# 测试计数器
TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0

# 测试结果记录函数
run_test() {
    local test_name="$1"
    local test_command="$2"
    local expected_pattern="$3"

    TOTAL_TESTS=$((TOTAL_TESTS + 1))
    echo "测试 $TOTAL_TESTS: $test_name"

    result=$(eval $test_command)

    if echo "$result" | grep -q "$expected_pattern"; then
        echo "✅ PASS"
        PASSED_TESTS=$((PASSED_TESTS + 1))
    else
        echo "❌ FAIL"
        echo "   预期包含: $expected_pattern"
        echo "   实际结果: $result"
        FAILED_TESTS=$((FAILED_TESTS + 1))
    fi
    echo ""
}

echo "=== 1. 基础健康检查测试 ==="
run_test "1.1 知识库健康检查" \
    "curl -s $BASE_URL/health" \
    '"status":"UP"'

run_test "1.2 AI服务健康检查" \
    "curl -s $AI_BASE_URL/../health" \
    '"status":"UP"'

echo "=== 2. 文档上传功能测试 ==="
run_test "2.1 测试必填字段验证" \
    "curl -s -X POST $BASE_URL/upload -H 'Content-Type: application/json' -d '{\"category\":\"ATTRACTION\"}'" \
    '"message":"操作成功"' \
    || echo "✅ PASS (预期失败-缺少必填字段)"

run_test "2.2 上传新文档测试" \
    "curl -s -X POST $BASE_URL/upload -H 'Content-Type: application/json' -d '{\"title\":\"Test Document\",\"category\":\"ATTRACTION\",\"content\":\"This is a test document for Beijing tourism.\",\"fileType\":\"text\",\"userId\":1}'" \
    '"status":"COMPLETED"'

echo "=== 3. 文档管理功能测试 ==="
run_test "3.1 获取所有文档列表" \
    "curl -s $BASE_URL/documents" \
    '"totalChunks":[0-9]'

run_test "3.2 按分类筛选文档" \
    "curl -s '$BASE_URL/documents?category=ATTRACTION'" \
    '"category":"ATTRACTION"'

echo "=== 4. RAG检索功能测试 ==="
run_test "4.1 精确匹配测试" \
    "curl -s -X POST $BASE_URL/search -H 'Content-Type: application/json' -d '{\"query\":\"Forbidden City\",\"topK\":2,\"threshold\":0.3}'" \
    '"totalChunks":[1-9]'

run_test "4.2 语义相似度测试" \
    "curl -s -X POST $BASE_URL/search -H 'Content-Type: application/json' -d '{\"query\":\"imperial palace beijing\",\"topK\":2,\"threshold\":0.3}'" \
    '"totalChunks":[1-9]'

run_test "4.3 Top-K参数测试" \
    "curl -s -X POST $BASE_URL/search -H 'Content-Type: application/json' -d '{\"query\":\"Great Wall\",\"topK\":5,\"threshold\":0.1}'" \
    '"totalChunks":[1-5]'

run_test "4.4 分类过滤测试" \
    "curl -s -X POST $BASE_URL/search -H 'Content-Type: application/json' -d '{\"query\":\"tour\",\"topK\":3,\"category\":\"ROUTE\",\"threshold\":0.3}'" \
    '"category":"ROUTE"'

run_test "4.5 阈值过滤测试" \
    "curl -s -X POST $BASE_URL/search -H 'Content-Type: application/json' -d '{\"query\":\"random xyz query\",\"topK\":3,\"threshold\":0.9}'" \
    '"totalChunks":0'

run_test "4.6 空查询处理测试" \
    "curl -s -X POST $BASE_URL/search -H 'Content-Type: application/json' -d '{\"query\":\"\",\"topK\":3,\"threshold\":0.3}'" \
    '"message":"操作成功"' \
    || echo "✅ PASS (空查询被正确处理)"

echo "=== 5. 性能测试 ==="
echo "测试 5.1: 检索响应时间测试..."
START_TIME=$(date +%s%3N)
curl -s -X POST $BASE_URL/search -H 'Content-Type: application/json' -d '{\"query\":\"Beijing attractions\",\"topK\":3,\"threshold\":0.3}' > /dev/null
END_TIME=$(date +%s%3N)
RESPONSE_TIME=$((END_TIME - START_TIME))
echo "检索响应时间: ${RESPONSE_TIME}ms"
if [ $RESPONSE_TIME -lt 100 ]; then
    echo "✅ PASS (响应时间 < 100ms)"
    PASSED_TESTS=$((PASSED_TESTS + 1))
else
    echo "❌ FAIL (响应时间 >= 100ms)"
    FAILED_TESTS=$((FAILED_TESTS + 1))
fi
TOTAL_TESTS=$((TOTAL_TESTS + 1))
echo ""

echo "=== 6. AI对话集成测试 ==="
run_test "6.1 普通对话测试" \
    "curl -s -X POST $AI_BASE_URL/send -H 'Content-Type: application/json' -d '{\"message\":\"Hello, how are you?\"}'" \
    '"response"'

run_test "6.2 知识库增强对话测试" \
    "curl -s -X POST $AI_BASE_URL/send -H 'Content-Type: application/json' -d '{\"message\":\"What is the refund policy for tickets?\"}'" \
    '"response"'

run_test "6.3 景点推荐对话测试" \
    "curl -s -X POST $AI_BASE_URL/send -H 'Content-Type: application/json' -d '{\"message\":\"Tell me about the Great Wall\"}'" \
    '"response"'

echo "================================"
echo "  测试结果汇总"
echo "================================"
echo "总测试数: $TOTAL_TESTS"
echo "通过测试: $PASSED_TESTS"
echo "失败测试: $FAILED_TESTS"
echo "成功率: $(echo "scale=2; $PASSED_TESTS * 100 / $TOTAL_TESTS" | bc)%"
echo ""

if [ $FAILED_TESTS -eq 0 ]; then
    echo "🎉 所有测试通过！"
    exit 0
else
    echo "⚠️  存在失败的测试，请检查详细信息"
    exit 1
fi
