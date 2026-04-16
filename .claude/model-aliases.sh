# GLM-5 模型选择便捷别名和函数
# 将此文件添加到 ~/.bashrc 或单独 source

# GLM-5 模型切换函数
glm() {
    case "$1" in
        5|default|"")
            echo "🔄 切换到 GLM-5 (默认)"
            export CURRENT_MODEL="GLM-5"
            ;;
        turbo|t)
            echo "⚡ 切换到 GLM-5-Turbo"
            export CURRENT_MODEL="GLM-5-Turbo"
            ;;
        plus|p)
            echo "🧠 切换到 GLM-5-Plus"
            export CURRENT_MODEL="GLM-5-Plus"
            ;;
        claude|c)
            echo "🤖 切换到 Claude-Sonnet"
            export CURRENT_MODEL="claude-sonnet-4-6"
            ;;
        haiku|h)
            echo "⚡️ 切换到 Claude-Haiku"
            export CURRENT_MODEL="claude-haiku-4-5"
            ;;
        status|s)
            echo "📊 当前模型: ${CURRENT_MODEL:-GLM-5}"
            ;;
        *)
            echo "❌ 无效的模型选项: $1"
            echo "   可用选项: 5/default, turbo/t, plus/p, claude/c, haiku/h, status/s"
            return 1
            ;;
    esac
}

# 快捷别名
alias glm5='glm 5'
alias glmt='glm turbo'
alias glmp='glm plus'
alias glms='glm status'

# 模型信息显示
model-info() {
    echo "🎯 GLM-5 模型信息"
    echo "=================="
    echo ""
    echo "当前模型: ${CURRENT_MODEL:-GLM-5}"
    echo ""
    echo "📋 可用模型:"
    echo "  GLM-5         - 平衡性能和质量 (默认)"
    echo "  GLM-5-Turbo   - 快速响应，简单任务"
    echo "  GLM-5-Plus    - 深度推理，复杂任务"
    echo "  Claude-Sonnet - Anthropic 备用模型"
    echo "  Claude-Haiku  - Anthropic 快速模型"
    echo ""
    echo "🚀 快速切换:"
    echo "  glm [5|turbo|plus]     # 切换模型"
    echo "  glm status             # 查看当前模型"
    echo ""
    echo "💡 使用示例:"
    echo "  glm turbo    # 切换到 GLM-5-Turbo"
    echo "  glmp         # 切换到 GLM-5-Plus"
    echo "  glms         # 查看当前模型"
}

# 模型对比
model-compare() {
    echo "📊 GLM-5 模型对比"
    echo "================="
    echo ""
    printf "%-15s %-8s %-8s %-8s %-20s\n" "模型" "速度" "质量" "成本" "适用场景"
    echo "─────────────────────────────────────────────────────────"
    printf "%-15s %-8s %-8s %-8s %-20s\n" "GLM-5" "⚡⚡" "⭐⭐⭐⭐" "💰💰" "日常开发"
    printf "%-15s %-8s %-8s %-8s %-20s\n" "GLM-5-Turbo" "⚡⚡⚡" "⭐⭐⭐" "💰" "快速问答"
    printf "%-15s %-8s %-8s %-8s %-20s\n" "GLM-5-Plus" "⚡" "⭐⭐⭐⭐⭐" "💰💰💰" "复杂任务"
    echo ""
}

# 添加到提示符 (可选)
show_model_in_prompt() {
    if [ -n "$CURRENT_MODEL" ]; then
        echo "[$CURRENT_MODEL]"
    fi
}

# 使用说明
usage() {
    echo "🎮 GLM-5 模型选择快捷命令"
    echo "=========================="
    echo ""
    echo "基础命令:"
    echo "  glm [选项]           # 切换模型"
    echo "  model-info           # 显示模型信息"
    echo "  model-compare        # 模型对比"
    echo ""
    echo "快捷别名:"
    echo "  glm5 / glmt / glmp   # 快速切换"
    echo "  glms                 # 查看状态"
    echo ""
    echo "模型选项:"
    echo "  5, default   - GLM-5 (默认)"
    echo "  turbo, t     - GLM-5-Turbo"
    echo "  plus, p      - GLM-5-Plus"
    echo "  claude, c    - Claude-Sonnet"
    echo "  haiku, h     - Claude-Haiku"
    echo ""
}

# 默认设置
export CURRENT_MODEL="${CURRENT_MODEL:-GLM-5}"

echo "✅ GLM-5 模型选择功能已加载"
echo "   输入 'glm' 或 'usage' 查看使用说明"
