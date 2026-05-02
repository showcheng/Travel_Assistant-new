-- User profile memory table
-- Stores extracted user preferences and travel profile from conversations
-- 论文 5.3.4: 用户画像记忆机制

CREATE TABLE IF NOT EXISTS user_profile (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL COMMENT '用户ID',
    profile_json JSON NOT NULL COMMENT '用户画像JSON',
    source_count INT DEFAULT 0 COMMENT '贡献来源对话数',
    last_conversation_id VARCHAR(64) DEFAULT NULL COMMENT '最近一次贡献的会话ID',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    version INT DEFAULT 1 COMMENT '乐观锁版本号',
    UNIQUE KEY uk_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户画像记忆表';
