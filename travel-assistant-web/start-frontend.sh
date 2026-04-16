#!/bin/bash
# 前端启动脚本
echo '请确保Node.js版本>=18.0.0'
node --version

# 设置NODE_OPTIONS以解决crypto问题
export NODE_OPTIONS=--openssl-legacy-provider

# 启动前端服务器
cd travel-assistant-web && npm run dev
