-- 智慧旅游助手平台数据库初始化脚本 (MySQL版本)

-- 使用数据库
USE travel_assistant;

-- ============================================
-- 用户模块
-- ============================================

-- 用户表
CREATE TABLE IF NOT EXISTS `user` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `phone` VARCHAR(20) UNIQUE NOT NULL,
    `password_hash` VARCHAR(255) NOT NULL,
    `nickname` VARCHAR(50),
    `avatar_url` VARCHAR(255),
    `register_source` VARCHAR(20),
    `status` SMALLINT DEFAULT 0 COMMENT '账号状态：0-正常，1-锁定',
    `deleted` SMALLINT DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
    `create_time` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `update_time` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_phone (`phone`),
    INDEX idx_user_status (`status`),
    INDEX idx_user_create_time (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- ============================================
-- 商品模块
-- ============================================

-- 商品分类表
CREATE TABLE IF NOT EXISTS `category` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `name` VARCHAR(50) NOT NULL,
    `parent_id` BIGINT DEFAULT 0,
    `level` SMALLINT DEFAULT 1,
    `sort` INT DEFAULT 0,
    `icon` VARCHAR(255),
    `deleted` SMALLINT DEFAULT 0,
    `create_time` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `update_time` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品分类表';

-- 商品表
CREATE TABLE IF NOT EXISTS `product` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `name` VARCHAR(200) NOT NULL,
    `category_id` BIGINT,
    `type` SMALLINT DEFAULT 1 COMMENT '类型：1-门票，2-商品',
    `price` DECIMAL(10, 2) NOT NULL,
    `original_price` DECIMAL(10, 2),
    `stock` INT DEFAULT 0,
    `sales` INT DEFAULT 0,
    `cover_image` VARCHAR(255),
    `detail` TEXT,
    `status` SMALLINT DEFAULT 1,
    `deleted` SMALLINT DEFAULT 0,
    `create_time` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `update_time` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_product_type (`type`),
    INDEX idx_product_status (`status`),
    INDEX idx_product_category (`category_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品表';

-- ============================================
-- 订单模块
-- ============================================

-- 订单表
CREATE TABLE IF NOT EXISTS `orders` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `order_no` VARCHAR(64) UNIQUE NOT NULL,
    `user_id` BIGINT NOT NULL,
    `total_amount` DECIMAL(10, 2) NOT NULL,
    `pay_amount` DECIMAL(10, 2) NOT NULL,
    `status` SMALLINT DEFAULT 0 COMMENT '状态：0-待支付，1-已支付，2-已取消，3-已退款',
    `pay_type` SMALLINT COMMENT '支付方式：1-支付宝，2-微信',
    `pay_time` TIMESTAMP NULL,
    `deleted` SMALLINT DEFAULT 0,
    `create_time` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `update_time` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_order_user (`user_id`),
    INDEX idx_order_status (`status`),
    INDEX idx_order_no (`order_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单表';

-- 订单明细表
CREATE TABLE IF NOT EXISTS `order_item` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `order_id` BIGINT NOT NULL,
    `product_id` BIGINT NOT NULL,
    `product_name` VARCHAR(200) NOT NULL,
    `price` DECIMAL(10, 2) NOT NULL,
    `quantity` INT NOT NULL,
    `total_amount` DECIMAL(10, 2) NOT NULL,
    `create_time` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_order_item_order (`order_id`),
    INDEX idx_order_item_product (`product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单明细表';

-- ============================================
-- 秒杀模块
-- ============================================

-- 秒杀商品表
CREATE TABLE IF NOT EXISTS `seckill_sku` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `product_id` BIGINT NOT NULL,
    `sku_name` VARCHAR(200) NOT NULL,
    `original_price` DECIMAL(10, 2) NOT NULL,
    `seckill_price` DECIMAL(10, 2) NOT NULL,
    `stock_count` INT NOT NULL DEFAULT 1000,
    `start_time` TIMESTAMP NOT NULL,
    `end_time` TIMESTAMP NOT NULL,
    `status` SMALLINT DEFAULT 1,
    `create_time` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `update_time` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_seckill_product (`product_id`),
    INDEX idx_seckill_time (`start_time`, `end_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='秒杀商品表';

-- 秒杀订单表
CREATE TABLE IF NOT EXISTS `seckill_order` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `order_no` VARCHAR(64) UNIQUE NOT NULL,
    `sku_id` BIGINT NOT NULL,
    `user_id` BIGINT NOT NULL,
    `status` SMALLINT DEFAULT 0,
    `money` DECIMAL(10, 2) NOT NULL,
    `create_time` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `pay_time` TIMESTAMP NULL,
    INDEX idx_seckill_order_user (`user_id`),
    INDEX idx_seckill_order_sku (`sku_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='秒杀订单表';

-- ============================================
-- 拼团模块
-- ============================================

-- 拼团表
CREATE TABLE IF NOT EXISTS `group_buy` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `product_id` BIGINT NOT NULL,
    `leader_user_id` BIGINT NOT NULL,
    `target_size` INT NOT NULL,
    `current_size` INT DEFAULT 1,
    `status` SMALLINT DEFAULT 0,
    `expire_time` TIMESTAMP NOT NULL,
    `create_time` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_group_buy_product (`product_id`),
    INDEX idx_group_buy_leader (`leader_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='拼团表';

-- 拼团成员表
CREATE TABLE IF NOT EXISTS `group_buy_member` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `group_id` BIGINT NOT NULL,
    `user_id` BIGINT NOT NULL,
    `order_id` BIGINT NOT NULL,
    `join_time` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_group_buy_member_group (`group_id`),
    INDEX idx_group_buy_member_user (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='拼团成员表';

-- ============================================
-- AI 模块
-- ============================================

-- AI 对话历史表
CREATE TABLE IF NOT EXISTS `chat_history` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL,
    `message` TEXT NOT NULL,
    `response` TEXT,
    `create_time` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_chat_history_user (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI对话历史表';

-- ============================================
-- 初始化数据
-- ============================================

-- 插入测试用户（密码：123456）
INSERT IGNORE INTO `user` (phone, password_hash, nickname, status)
VALUES ('13800138000', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '测试用户', 0);

-- 插入商品分类
INSERT IGNORE INTO `category` (name, parent_id, level) VALUES
    ('景点门票', 0, 1),
    ('旅游商品', 0, 1),
    ('酒店住宿', 0, 1);

-- 插入示例商品
INSERT IGNORE INTO `product` (name, category_id, type, price, original_price, stock, cover_image, detail, status) VALUES
    ('故宫博物院成人票', 1, 1, 60.00, 80.00, 1000, 'https://example.com/images/forbidden-city.jpg', '北京故宫博物院成人门票，包含故宫主要景点参观权限', 1),
    ('长城一日游', 1, 1, 188.00, 238.00, 500, 'https://example.com/images/great-wall.jpg', '八达岭长城一日游，含交通和导游服务', 1),
    ('天坛公园门票', 1, 1, 35.00, 45.00, 2000, 'https://example.com/images/temple-of-heaven.jpg', '北京天坛公园联票，包含祈年殿、回音壁等景点', 1),
    ('北京特产礼盒', 2, 2, 128.00, 168.00, 300, 'https://example.com/images/gift-box.jpg', '北京特产礼盒，包含烤鸭、果脯等特色食品', 1),
    ('故宫文创纪念品', 2, 2, 58.00, 78.00, 800, 'https://example.com/images/souvenir.jpg', '故宫文创纪念品，包含书签、明信片等', 1);
