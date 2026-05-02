package com.travel.ai.controller;

import com.travel.ai.dto.DocumentUploadRequest;
import com.travel.ai.dto.DocumentUploadResponse;
import com.travel.ai.dto.RAGSearchRequest;
import com.travel.ai.dto.RAGSearchResponse;
import com.travel.ai.service.KnowledgeBaseService;
import com.travel.ai.util.FileParser;
import com.travel.common.response.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * 知识库管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/knowledge")
@RequiredArgsConstructor
@Tag(name = "知识库管理", description = "知识库文档管理和RAG检索接口")
public class KnowledgeBaseController {

    private final KnowledgeBaseService knowledgeBaseService;

    /**
     * 上传文档到知识库
     */
    @PostMapping("/upload")
    @Operation(summary = "上传文档", description = "上传文档到知识库并进行向量化处理")
    public Result<DocumentUploadResponse> uploadDocument(@Valid @RequestBody DocumentUploadRequest request) {
        try {
            log.info("收到文档上传请求: title={}, category={}", request.getTitle(), request.getCategory());

            DocumentUploadResponse response = knowledgeBaseService.uploadDocument(request);

            log.info("文档上传成功: docId={}, chunkCount={}", response.getDocId(), response.getChunkCount());
            return Result.success(response);

        } catch (Exception e) {
            log.error("文档上传失败", e);
            return Result.error("文档上传失败: " + e.getMessage());
        }
    }

    /**
     * 上传文件到知识库（支持PDF和TXT）
     */
    @PostMapping(value = "/upload/file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "上传文件", description = "上传PDF或TXT文件到知识库并进行向量化处理")
    public Result<?> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "userId", required = false) Long userId) {

        // Validate file is not empty
        if (file.isEmpty()) {
            return Result.error("文件不能为空");
        }

        String filename = file.getOriginalFilename();
        FileParser parser = new FileParser();

        // Validate file extension
        if (!parser.isSupported(filename)) {
            return Result.error("仅支持PDF和TXT格式");
        }

        try {
            // Parse file content
            String content = parser.parse(filename, file.getInputStream());

            // Use filename as title if not provided
            String docTitle = (title != null && !title.isEmpty()) ? title : filename;

            // Use default category if not provided
            String docCategory = (category != null && !category.isEmpty()) ? category : "未分类";

            // Use default userId if not provided
            Long docUserId = (userId != null) ? userId : 1L;

            // Build upload request and delegate to service
            DocumentUploadRequest uploadRequest = new DocumentUploadRequest();
            uploadRequest.setTitle(docTitle);
            uploadRequest.setContent(content);
            uploadRequest.setCategory(docCategory);
            uploadRequest.setFileType(filename.substring(filename.lastIndexOf('.') + 1));
            uploadRequest.setUserId(docUserId);

            DocumentUploadResponse response = knowledgeBaseService.uploadDocument(uploadRequest);

            log.info("文件上传成功: filename={}, docId={}", filename, response.getDocId());
            return Result.success(response);

        } catch (IllegalArgumentException e) {
            log.warn("文件上传验证失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("文件处理失败: filename={}", filename, e);
            return Result.error("文件处理失败: " + e.getMessage());
        }
    }

    /**
     * RAG检索
     */
    @PostMapping("/search")
    @Operation(summary = "RAG检索", description = "基于向量相似度检索知识库内容")
    public Result<RAGSearchResponse> search(@Valid @RequestBody RAGSearchRequest request) {
        try {
            log.info("收到RAG检索请求: query={}, topK={}", request.getQuery(), request.getTopK());

            RAGSearchResponse response = knowledgeBaseService.search(request);

            log.info("RAG检索完成: query={}, results={}", request.getQuery(), response.getTotalChunks());
            return Result.success(response);

        } catch (Exception e) {
            log.error("RAG检索失败", e);
            return Result.error("RAG检索失败: " + e.getMessage());
        }
    }

    /**
     * 删除文档
     */
    @DeleteMapping("/document/{docId}")
    @Operation(summary = "删除文档", description = "从知识库删除指定文档及其向量数据")
    public Result<Void> deleteDocument(@PathVariable String docId) {
        try {
            log.info("删除文档: docId={}", docId);

            boolean success = knowledgeBaseService.deleteDocument(docId);

            if (success) {
                log.info("文档删除成功: docId={}", docId);
                return Result.success();
            } else {
                return Result.error("文档删除失败");
            }

        } catch (Exception e) {
            log.error("文档删除失败", e);
            return Result.error("文档删除失败: " + e.getMessage());
        }
    }

    /**
     * 获取文档列表
     */
    @GetMapping("/documents")
    @Operation(summary = "获取文档列表", description = "获取知识库中的文档列表，支持分类过滤")
    public Result<List<Object>> listDocuments(
            @RequestParam(required = false) String category) {
        try {
            log.info("获取文档列表: category={}", category);

            List<Object> documents = knowledgeBaseService.listDocuments(category);

            log.info("文档列表获取成功: count={}", documents.size());
            return Result.success(documents);

        } catch (Exception e) {
            log.error("获取文档列表失败", e);
            return Result.error("获取文档列表失败: " + e.getMessage());
        }
    }

    /**
     * 初始化知识库
     */
    @PostMapping("/initialize")
    @Operation(summary = "初始化知识库", description = "初始化Milvus集合和索引")
    public Result<Map<String, Object>> initialize() {
        try {
            log.info("开始初始化知识库");

            knowledgeBaseService.initializeCollection();

            Map<String, Object> result = Map.of(
                    "message", "知识库初始化成功",
                    "timestamp", System.currentTimeMillis()
            );

            log.info("知识库初始化完成");
            return Result.success(result);

        } catch (Exception e) {
            log.error("知识库初始化失败", e);
            return Result.error("知识库初始化失败: " + e.getMessage());
        }
    }

    /**
     * 健康检查
     */
    @GetMapping("/health")
    @Operation(summary = "健康检查", description = "检查知识库服务状态")
    public Result<Map<String, Object>> health() {
        Map<String, Object> healthInfo = Map.of(
                "status", "UP",
                "features", Map.of(
                        "document_upload", true,
                        "vector_search", true,
                        "rag_retrieval", true
                ),
                "timestamp", System.currentTimeMillis()
        );
        return Result.success(healthInfo);
    }
}
