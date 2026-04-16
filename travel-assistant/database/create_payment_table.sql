-- 创建支付表
USE travel_assistant;

CREATE TABLE IF NOT EXISTS `payment` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `payment_no` VARCHAR(64) UNIQUE NOT NULL COMMENT '支付单号',
    `order_id` BIGINT NOT NULL COMMENT '订单ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `amount` DECIMAL(10, 2) NOT NULL COMMENT '支付金额',
    `pay_type` SMALLINT DEFAULT 1 COMMENT '支付方式：1-支付宝，2-微信，3-余额',
    `status` SMALLINT DEFAULT 0 COMMENT '支付状态：0-待支付，1-支付中，2-支付成功，3-支付失败',
    `transaction_id` VARCHAR(64) COMMENT '第三方交易号',
    `pay_time` TIMESTAMP NULL COMMENT '支付时间',
    `deleted` SMALLINT DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
    `create_time` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_payment_order (`order_id`),
    INDEX idx_payment_user (`user_id`),
    INDEX idx_payment_no (`payment_no`),
    INDEX idx_payment_status (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付表';
