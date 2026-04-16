#!/bin/bash

# 设置 GLM-5 为项目默认模型的脚本

echo "🔧 设置 GLM-5 为会话默认模型"
echo "================================"
echo ""

# 项目路径（标准化）
PROJECT_PATH="d:/JAVA_Porject/Travel_Assistant-new"
CONFIG_FILE="$HOME/.claude.json"

echo "📋 项目路径: $PROJECT_PATH"
echo "📋 配置文件: $CONFIG_FILE"
echo ""

# 备份配置文件
echo "📦 备份原配置文件..."
cp "$CONFIG_FILE" "$CONFIG_FILE.backup.$(date +%Y%m%d_%H%M%S)"
echo "✅ 备份完成"
echo ""

# 检查项目是否已存在配置中
if grep -q "$PROJECT_PATH" "$CONFIG_FILE"; then
    echo "✅ 找到现有项目配置"
    echo "📝 需要更新默认模型设置"
else
    echo "⚠️  项目不在配置文件中"
    echo "📝 需要添加项目配置"
fi

echo ""
echo "🎯 配置目标:"
echo "   在项目配置中添加: \"defaultModel\": \"GLM-5\""
echo ""

echo "💡 手动配置步骤:"
echo "1. 打开 ~/.claude.json"
echo "2. 找到 \"$PROJECT_PATH\" 项目配置"
echo "3. 在项目配置中添加或修改:"
echo "   \"defaultModel\": \"GLM-5\","
echo "   \"modelPreferences\": {"
echo "     \"primary\": \"GLM-5\","
echo "     \"fallback\": \"claude-sonnet-4-6\""
echo "   }"
echo ""

echo "🔄 或者使用对话指定:"
echo "   每次会话开始时说: \"使用 GLM-5 模型\""
echo ""

echo "📖 详细说明: .claude/README.md"
