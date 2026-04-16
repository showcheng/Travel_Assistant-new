#!/bin/bash

# AI服务启动脚本

echo "=========================================="
echo "  智慧旅游助手 - AI 服务"
echo "=========================================="

# 检查GLM-5 API密钥
if [ -z "$GLM5_API_KEY" ]; then
    echo "警告: 未设置GLM5_API_KEY环境变量"
    echo "请设置: export GLM5_API_KEY=your-api-key"
    echo ""
fi

# 进入模块目录
cd "$(dirname "$0")"

# 启动AI服务
echo "正在启动AI服务..."
mvn spring-boot:run

echo "AI服务已停止"
