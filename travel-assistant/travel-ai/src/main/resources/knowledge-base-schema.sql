-- 知识库文档表
CREATE TABLE IF NOT EXISTS knowledge_document (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    doc_id VARCHAR(64) NOT NULL UNIQUE COMMENT '文档ID',
    title VARCHAR(255) NOT NULL COMMENT '文档标题',
    category VARCHAR(100) COMMENT '文档分类',
    file_type VARCHAR(20) COMMENT '文件类型',
    file_size BIGINT COMMENT '文件大小(字节)',
    file_path VARCHAR(500) COMMENT '文件存储路径',
    status VARCHAR(20) DEFAULT 'PROCESSING' COMMENT '处理状态: PROCESSING, COMPLETED, FAILED',
    chunk_count INT DEFAULT 0 COMMENT '分块数量',
    upload_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
    processed_time DATETIME COMMENT '处理完成时间',
    created_by BIGINT COMMENT '创建者用户ID',
    INDEX idx_category (category),
    INDEX idx_status (status),
    INDEX idx_upload_time (upload_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识库文档表';

-- 文档分块表
CREATE TABLE IF NOT EXISTS knowledge_chunk (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    chunk_id VARCHAR(64) NOT NULL UNIQUE COMMENT '分块ID',
    doc_id VARCHAR(64) NOT NULL COMMENT '文档ID',
    chunk_index INT NOT NULL COMMENT '分块序号',
    content TEXT NOT NULL COMMENT '分块内容',
    metadata JSON COMMENT '元数据',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_doc_id (doc_id),
    INDEX idx_chunk_index (chunk_index),
    FOREIGN KEY (doc_id) REFERENCES knowledge_document(doc_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文档分块表';

-- 向量存储记录表
CREATE TABLE IF NOT EXISTS knowledge_vector (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    chunk_id VARCHAR(64) NOT NULL COMMENT '分块ID',
    vector_id VARCHAR(64) COMMENT 'Milvus中的向量ID',
    dimension INT DEFAULT 768 COMMENT '向量维度',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_chunk_id (chunk_id),
    UNIQUE KEY uk_chunk_id (chunk_id),
    FOREIGN KEY (chunk_id) REFERENCES knowledge_chunk(chunk_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='向量存储记录表';

-- 知识库分类表
CREATE TABLE IF NOT EXISTS knowledge_category (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    category_code VARCHAR(50) NOT NULL UNIQUE COMMENT '分类代码',
    category_name VARCHAR(100) NOT NULL COMMENT '分类名称',
    description VARCHAR(500) COMMENT '分类描述',
    parent_id BIGINT DEFAULT 0 COMMENT '父分类ID',
    sort_order INT DEFAULT 0 COMMENT '排序',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_parent_id (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识库分类表';

-- 插入初始分类数据
INSERT INTO knowledge_category (category_code, category_name, description, sort_order) VALUES
('ATTRACTION', '景点介绍', '旅游景点相关介绍', 1),
('POLICY', '政策说明', '退改签、购票等政策', 2),
('ROUTE', '路线推荐', '旅游路线推荐', 3),
('FAQ', '常见问题', '用户常见问题', 4),
('HOTEL', '酒店住宿', '酒店住宿相关', 5),
('TRANSPORT', '交通指南', '交通方式指南', 6);
