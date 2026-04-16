-- 为商品表添加乐观锁版本字段
ALTER TABLE product ADD COLUMN version INT DEFAULT 0 COMMENT '乐观锁版本号' AFTER deleted;

-- 为现有记录设置初始版本值
UPDATE product SET version = 0 WHERE version IS NULL;

-- 添加索引
ALTER TABLE product ADD INDEX idx_version (version);
