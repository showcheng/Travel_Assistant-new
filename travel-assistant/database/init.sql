-- 智慧旅游助手平台数据库初始化脚本

-- 创建数据库
CREATE DATABASE IF NOT EXISTS travel_assistant
    WITH
    OWNER = postgres
    ENCODING = 'UTF8'
    LC_COLLATE = 'zh_CN.UTF-8'
    LC_CTYPE = 'zh_CN.UTF-8'
    TABLESPACE = pg_default
    CONNECTION LIMIT = -1;

-- 使用数据库
\c travel_assistant;

-- ============================================
-- 用户模块
-- ============================================

-- 用户表
CREATE TABLE IF NOT EXISTS "user" (
    "id" BIGSERIAL PRIMARY KEY,
    "phone" VARCHAR(20) UNIQUE NOT NULL,
    "password_hash" VARCHAR(255) NOT NULL,
    "nickname" VARCHAR(50),
    "avatar_url" VARCHAR(255),
    "register_source" VARCHAR(20),
    "status" SMALLINT DEFAULT 0,
    "deleted" SMALLINT DEFAULT 0,
    "create_time" TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    "update_time" TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE "user" IS '用户表';
COMMENT ON COLUMN "user"."id" IS '用户ID';
COMMENT ON COLUMN "user"."phone" IS '手机号';
COMMENT ON COLUMN "user"."password_hash" IS '密码（BCrypt加密）';
COMMENT ON COLUMN "user"."nickname" IS '昵称';
COMMENT ON COLUMN "user"."avatar_url" IS '头像URL';
COMMENT ON COLUMN "user"."register_source" IS '注册来源';
COMMENT ON COLUMN "user"."status" IS '账号状态：0-正常，1-锁定';
COMMENT ON COLUMN "user"."deleted" IS '逻辑删除：0-未删除，1-已删除';

-- 创建索引
CREATE INDEX idx_user_phone ON "user"(phone);
CREATE INDEX idx_user_status ON "user"(status);
CREATE INDEX idx_user_create_time ON "user"(create_time);

-- ============================================
-- 商品模块
-- ============================================

-- 商品分类表
CREATE TABLE IF NOT EXISTS "category" (
    "id" BIGSERIAL PRIMARY KEY,
    "name" VARCHAR(50) NOT NULL,
    "parent_id" BIGINT DEFAULT 0,
    "level" SMALLINT DEFAULT 1,
    "sort" INT DEFAULT 0,
    "icon" VARCHAR(255),
    "deleted" SMALLINT DEFAULT 0,
    "create_time" TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    "update_time" TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE "category" IS '商品分类表';

-- 商品表
CREATE TABLE IF NOT EXISTS "product" (
    "id" BIGSERIAL PRIMARY KEY,
    "name" VARCHAR(200) NOT NULL,
    "category_id" BIGINT,
    "type" SMALLINT DEFAULT 1,
    "price" DECIMAL(10, 2) NOT NULL,
    "original_price" DECIMAL(10, 2),
    "stock" INT DEFAULT 0,
    "sales" INT DEFAULT 0,
    "cover_image" VARCHAR(255),
    "detail" TEXT,
    "status" SMALLINT DEFAULT 1,
    "deleted" SMALLINT DEFAULT 0,
    "create_time" TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    "update_time" TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE "product" IS '商品表';
COMMENT ON COLUMN "product"."type" IS '类型：1-门票，2-商品';

-- ============================================
-- 订单模块
-- ============================================

-- 订单表
CREATE TABLE IF NOT EXISTS "orders" (
    "id" BIGSERIAL PRIMARY KEY,
    "order_no" VARCHAR(64) UNIQUE NOT NULL,
    "user_id" BIGINT NOT NULL,
    "total_amount" DECIMAL(10, 2) NOT NULL,
    "pay_amount" DECIMAL(10, 2) NOT NULL,
    "status" SMALLINT DEFAULT 0,
    "pay_type" SMALLINT,
    "pay_time" TIMESTAMP,
    "deleted" SMALLINT DEFAULT 0,
    "create_time" TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    "update_time" TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE "orders" IS '订单表';
COMMENT ON COLUMN "orders"."status" IS '状态：0-待支付，1-已支付，2-已取消，3-已退款';
COMMENT ON COLUMN "orders"."pay_type" IS '支付方式：1-支付宝，2-微信';

-- 订单明细表
CREATE TABLE IF NOT EXISTS "order_item" (
    "id" BIGSERIAL PRIMARY KEY,
    "order_id" BIGINT NOT NULL,
    "product_id" BIGINT NOT NULL,
    "product_name" VARCHAR(200) NOT NULL,
    "price" DECIMAL(10, 2) NOT NULL,
    "quantity" INT NOT NULL,
    "total_amount" DECIMAL(10, 2) NOT NULL,
    "create_time" TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE "order_item" IS '订单明细表';

-- ============================================
-- 秒杀模块
-- ============================================

-- 秒杀商品表
CREATE TABLE IF NOT EXISTS "seckill_sku" (
    "id" BIGSERIAL PRIMARY KEY,
    "product_id" BIGINT NOT NULL,
    "sku_name" VARCHAR(200) NOT NULL,
    "original_price" DECIMAL(10, 2) NOT NULL,
    "seckill_price" DECIMAL(10, 2) NOT NULL,
    "stock_count" INT NOT NULL DEFAULT 1000,
    "start_time" TIMESTAMP NOT NULL,
    "end_time" TIMESTAMP NOT NULL,
    "status" SMALLINT DEFAULT 1,
    "create_time" TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    "update_time" TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE "seckill_sku" IS '秒杀商品表';

-- 秒杀订单表
CREATE TABLE IF NOT EXISTS "seckill_order" (
    "id" BIGSERIAL PRIMARY KEY,
    "order_no" VARCHAR(64) UNIQUE NOT NULL,
    "sku_id" BIGINT NOT NULL,
    "user_id" BIGINT NOT NULL,
    "status" SMALLINT DEFAULT 0,
    "money" DECIMAL(10, 2) NOT NULL,
    "create_time" TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    "pay_time" TIMESTAMP
);

COMMENT ON TABLE "seckill_order" IS '秒杀订单表';

-- ============================================
-- 拼团模块
-- ============================================

-- 拼团表
CREATE TABLE IF NOT EXISTS "group_buy" (
    "id" BIGSERIAL PRIMARY KEY,
    "product_id" BIGINT NOT NULL,
    "leader_user_id" BIGINT NOT NULL,
    "target_size" INT NOT NULL,
    "current_size" INT DEFAULT 1,
    "status" SMALLINT DEFAULT 0,
    "expire_time" TIMESTAMP NOT NULL,
    "create_time" TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE "group_buy" IS '拼团表';

-- 拼团成员表
CREATE TABLE IF NOT EXISTS "group_buy_member" (
    "id" BIGSERIAL PRIMARY KEY,
    "group_id" BIGINT NOT NULL,
    "user_id" BIGINT NOT NULL,
    "order_id" BIGINT NOT NULL,
    "join_time" TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE "group_buy_member" IS '拼团成员表';

-- ============================================
-- AI 模块
-- ============================================

-- AI 对话历史表
CREATE TABLE IF NOT EXISTS "chat_history" (
    "id" BIGSERIAL PRIMARY KEY,
    "user_id" BIGINT NOT NULL,
    "message" TEXT NOT NULL,
    "response" TEXT,
    "create_time" TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE "chat_history" IS 'AI对话历史表';

-- ============================================
-- 初始化数据
-- ============================================

-- 插入测试用户（密码：123456）
INSERT INTO "user" (phone, password_hash, nickname, status)
VALUES ('13800138000', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '测试用户', 0)
ON CONFLICT (phone) DO NOTHING;

-- 插入商品分类
INSERT INTO "category" (name, parent_id, level) VALUES
    ('景点门票', 0, 1),
    ('旅游商品', 0, 1),
    ('酒店住宿', 0, 1)
ON CONFLICT DO NOTHING;

-- ============================================
-- 创建函数：自动更新 update_time
-- ============================================

CREATE OR REPLACE FUNCTION update_updated_time_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.update_time = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- 为所有表创建触发器
CREATE TRIGGER update_user_update_time BEFORE UPDATE ON "user"
    FOR EACH ROW EXECUTE FUNCTION update_updated_time_column();

CREATE TRIGGER update_product_update_time BEFORE UPDATE ON product
    FOR EACH ROW EXECUTE FUNCTION update_updated_time_column();

CREATE TRIGGER update_orders_update_time BEFORE UPDATE ON orders
    FOR EACH ROW EXECUTE FUNCTION update_updated_time_column();

CREATE TRIGGER update_seckill_sku_update_time BEFORE UPDATE ON seckill_sku
    FOR EACH ROW EXECUTE FUNCTION update_updated_time_column();
