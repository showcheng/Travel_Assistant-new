# GLM-5 模型配置 - 极简使用说明

## ⚡ 会话默认使用 GLM-5

### 🎯 方法 1: 对话指定 (推荐)

**每次新会话开始时说：**
```
使用 GLM-5 模型
```

✅ 最简单，无需配置，立即可用！

---

## 🔧 永久配置 (可选)

### 修改主配置文件

1. **打开配置文件**
   ```bash
   code ~/.claude.json
   ```

2. **在 projects 部分添加**
   ```json
   "d:/JAVA_Porject/Travel_Assistant-new": {
     "defaultModel": "GLM-5"
   }
   ```

3. **重启 Claude Code**

---

## 💬 使用方法

### 对话中指定模型
```
使用 GLM-5 模型创建 API
切换到 GLM-5-Turbo 快速回答
用 GLM-5-Plus 深入分析
```

### 使用便捷命令
```bash
source .claude/model-aliases.sh
glm turbo    # 快速模式
glm plus     # 深度模式
glms         # 查看状态
```

---

## 🧪 验证配置

```bash
# 测试 API 连接
source .claude/test-glm-api-v2.sh

# 检查配置状态
cat .claude/.clauderc
```

---

## 📊 模型对比

| 模型 | 速度 | 质量 | 适用 |
|------|------|------|------|
| GLM-5 | ⚡⚡ | ⭐⭐⭐⭐ | 日常开发 |
| GLM-5-Turbo | ⚡⚡⚡ | ⭐⭐⭐ | 快速问答 |
| GLM-5-Plus | ⚡ | ⭐⭐⭐⭐⭐ | 深度分析 |

---

## 📁 配置文件

- **`.claude/.clauderc`**: 模型配置
- **`.claude/model-aliases.sh`**: 便捷命令
- **`.claude/test-glm-api-v2.sh`**: API 测试
- **`~/.bashrc`**: API 密钥配置

---

**🎯 会话开始时说 "使用 GLM-5 模型" 即可！**
