package com.travel.ai.service;

import com.travel.ai.dto.DocumentUploadRequest;
import com.travel.ai.dto.DocumentUploadResponse;
import com.travel.ai.dto.RAGSearchRequest;
import com.travel.ai.dto.RAGSearchResponse;

import java.util.List;

/**
 * 知识库服务接口
 */
public interface KnowledgeBaseService {

    /**
     * 上传文档到知识库
     *
     * @param request 文档上传请求
     * @return 上传响应
     */
    DocumentUploadResponse uploadDocument(DocumentUploadRequest request);

    /**
     * RAG检索
     *
     * @param request 检索请求
     * @return 检索响应
     */
    RAGSearchResponse search(RAGSearchRequest request);

    /**
     * 删除文档
     *
     * @param docId 文档ID
     * @return 是否成功
     */
    boolean deleteDocument(String docId);

    /**
     * 获取文档列表
     *
     * @param category 分类（可选）
     * @return 文档列表
     */
    List<Object> listDocuments(String category);

    /**
     * 初始化Milvus集合
     */
    void initializeCollection();
}
