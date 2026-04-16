-- 创建订单明细表
USE travel_assistant;

-- 删除已存在的order_item表（如果存在）
DROP TABLE IF EXISTS `order_item`;

CREATE TABLE IF NOT EXISTS `order_item` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `order_id` BIGINT NOT NULL COMMENT '订单ID',
    `product_id` BIGINT NOT NULL COMMENT '商品ID',
    `product_name` VARCHAR(255) NOT NULL COMMENT '商品名称',
    `product_image` VARCHAR(512) COMMENT '商品图片',
    `quantity` INT NOT NULL COMMENT '购买数量',
    `price` DECIMAL(10, 2) NOT NULL COMMENT '单价',
    `total_price` DECIMAL(10, 2) NOT NULL COMMENT '小计金额',
    `deleted` SMALLINT DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
    `create_time` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_order_id (`order_id`),
    INDEX idx_product_id (`product_id`),
    INDEX idx_order_product (`order_id`, `product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单明细表';

-- 插入测试数据
INSERT INTO `order_item` (order_id, product_id, product_name, product_image, quantity, price, total_price) VALUES
(5, 1, '故宫博物院成人票', 'https://example.com/images/forbidden-city.jpg', 2, 60.00, 120.00),
(5, 2, '北京特产礼盒', 'https://example.com/images/gift-box.jpg', 1, 128.00, 128.00);
