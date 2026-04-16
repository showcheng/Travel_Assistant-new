# GLM-5 模型配置

> 本项目默认使用智谱 GLM-5 模型

## ⚡ 立即使用

```bash
# 加载功能
source .claude/model-aliases.sh

# 切换模型
glm turbo    # 快速模式
glm plus     # 深度模式
glms         # 查看状态
```

## 💬 对话中直接指定

```
使用 GLM-5 模型创建 API
切换到 GLM-5-Turbo 快速回答
用 GLM-5-Plus 深入分析
```

## 📁 配置文件

所有配置文件位于 `.claude/` 目录：
- **README.md** - 详细使用说明
- **model-aliases.sh** - 模型切换命令
- **test-glm-api-v2.sh** - API 连接测试
- **.clauderc** - 模型配置文件

## 🧪 验证配置

```bash
source .claude/test-glm-api-v2.sh
```

---

**🎯 默认模型: GLM-5 | 随时切换 | 查看说明: `.claude/README.md`**
