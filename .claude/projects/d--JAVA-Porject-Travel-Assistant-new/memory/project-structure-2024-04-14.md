---
name: 项目结构记录
description: 智慧旅游助手项目目录结构和规范分析
type: project

---

# 项目结构记录 - 2026-04-14

## 当前项目结构

### 主项目目录
`d:/JAVA_Porject/Travel_Assistant-new/travel-assistant/`

### 模块列表
1. travel-common - 公共模块 (100% 完成)
2. travel-user - 用户服务 (90% 完成)
3. travel-product - 商品服务 (20% 完成)
4. travel-order - 订单服务 (20% 完成)
5. travel-seckill - 秒杀服务 (20% 完成)
6. travel-group - 拼团服务 (20% 完成)
7. travel-ai - AI 服务 (20% 完成)
8. travel-live - 数字人直播 (20% 完成)
9. travel-gateway - 网关服务 (20% 完成)

## 技术栈
- Java 17
- Spring Boot 3.2.0
- Maven 多模块
- PostgreSQL 15
- Redis 7.0
- Kafka 3.6

## 目录结构规范
符合 Spring Boot 标准目录布局，包命名规范：`com.travel.{module}.{layer}`

## 已知问题
1. 根目录下有重复的空模块目录（需删除）
2. 大部分模块缺少完整的包结构（entity、dto、mapper、service）

## 改进建议
1. 清理重复目录
2. 补充缺失的 application.yml
3. 完善包结构
