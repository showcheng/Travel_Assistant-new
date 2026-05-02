-- AI服务数据库表结构

-- 对话会话表
CREATE TABLE IF NOT EXISTS conversation_session (
    id BIGINT PRIMARY KEY,
    session_id VARCHAR(64) NOT NULL UNIQUE COMMENT '会话唯一标识',
    user_id BIGINT COMMENT '用户ID',
    title VARCHAR(200) COMMENT '会话标题',
    status VARCHAR(20) DEFAULT 'ACTIVE' COMMENT '会话状态',
    message_count INT DEFAULT 0 COMMENT '消息数量',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    expired_at TIMESTAMP COMMENT '过期时间',
    INDEX idx_session_id (session_id),
    INDEX idx_user_id (user_id),
    INDEX idx_expired_at (expired_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='对话会话表';

-- 对话消息表
CREATE TABLE IF NOT EXISTS conversation_message (
    id BIGINT PRIMARY KEY,
    session_id VARCHAR(64) NOT NULL COMMENT '会话ID',
    user_id BIGINT COMMENT '用户ID',
    role VARCHAR(20) NOT NULL COMMENT '消息角色',
    content TEXT NOT NULL COMMENT '消息内容',
    tokens INT COMMENT 'Token数量',
    intent_type VARCHAR(50) COMMENT '意图类型',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_session_id (session_id),
    INDEX idx_user_id (user_id),
    INDEX idx_created_at (created_at),
    FOREIGN KEY (session_id) REFERENCES conversation_session(session_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='对话消息表';

-- 用户画像记忆表 (论文 5.3.4: 用户画像记忆机制)
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
