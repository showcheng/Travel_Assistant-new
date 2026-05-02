package com.travel.ai.service.impl;

import com.travel.ai.dto.*;
import com.travel.ai.entity.*;
import com.travel.ai.mapper.*;
import com.travel.ai.service.KnowledgeBaseService;
import com.travel.ai.store.VectorStore;
import com.travel.ai.util.TextChunker;
import dev.langchain4j.model.embedding.EmbeddingModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Knowledge base service implementation.
 * Delegates vector operations to the VectorStore interface,
 * which may be backed by Milvus or in-memory storage.
 */
@Slf4j
@Service("knowledgeBaseService")
@RequiredArgsConstructor
public class KnowledgeBaseServiceSimpleImpl implements KnowledgeBaseService {

    private final EmbeddingModel embeddingModel;
    private final TextChunker textChunker;
    private final KnowledgeDocumentMapper documentMapper;
    private final KnowledgeChunkMapper chunkMapper;
    private final KnowledgeVectorMapper vectorMapper;
    private final VectorStore vectorStore;

    @Override
    @Transactional
    public DocumentUploadResponse uploadDocument(DocumentUploadRequest request) {
        try {
            // 1. Create document record
            String docId = UUID.randomUUID().toString().replace("-", "");
            KnowledgeDocument document = new KnowledgeDocument();
            document.setDocId(docId);
            document.setTitle(request.getTitle());
            document.setCategory(request.getCategory());
            document.setFileType(request.getFileType());
            document.setStatus("PROCESSING");
            document.setUploadTime(LocalDateTime.now());
            document.setCreatedBy(request.getUserId());

            documentMapper.insert(document);
            log.info("Document record created: docId={}, title={}", docId, request.getTitle());

            // 2. Text chunking
            List<String> chunks = textChunker.chunkText(request.getContent());
            log.info("Document chunking complete: docId={}, chunkCount={}", docId, chunks.size());

            // 3. Process each chunk
            for (int i = 0; i < chunks.size(); i++) {
                String chunkId = UUID.randomUUID().toString().replace("-", "");
                String content = chunks.get(i);

                // Save chunk to database
                KnowledgeChunk chunk = new KnowledgeChunk();
                chunk.setChunkId(chunkId);
                chunk.setDocId(docId);
                chunk.setChunkIndex(i);
                chunk.setContent(content);
                chunk.setCreatedAt(LocalDateTime.now());

                chunkMapper.insert(chunk);

                // Generate embedding vector
                float[] vectorArray = embeddingModel.embed(content).content().vector();
                List<Float> vector = new ArrayList<>();
                for (float v : vectorArray) {
                    vector.add(v);
                }

                // Build metadata for the vector store
                Map<String, String> metadata = new HashMap<>();
                metadata.put("doc_id", docId);
                metadata.put("doc_title", request.getTitle());
                metadata.put("content", content);
                metadata.put("category", request.getCategory());
                metadata.put("chunk_index", String.valueOf(i));

                // Delegate to VectorStore (Milvus or in-memory)
                vectorStore.upsert(chunkId, vector, metadata);

                log.debug("Chunk vectorized: chunkId={}, index={}, vectorSize={}",
                         chunkId, i, vector.size());
            }

            // 4. Update document status
            document.setStatus("COMPLETED");
            document.setChunkCount(chunks.size());
            document.setProcessedTime(LocalDateTime.now());
            documentMapper.updateById(document);

            log.info("Document upload complete: docId={}, chunkCount={}", docId, chunks.size());

            return DocumentUploadResponse.builder()
                    .docId(docId)
                    .title(request.getTitle())
                    .status("COMPLETED")
                    .chunkCount(chunks.size())
                    .message("Document uploaded successfully")
                    .uploadTime(document.getUploadTime())
                    .build();

        } catch (Exception e) {
            log.error("Document upload failed: title={}", request.getTitle(), e);
            throw new RuntimeException("Document upload failed: " + e.getMessage(), e);
        }
    }

    @Override
    public RAGSearchResponse search(RAGSearchRequest request) {
        try {
            // 1. Generate query vector
            float[] queryVectorArray = embeddingModel.embed(request.getQuery())
                    .content().vector();
            List<Float> queryVector = new ArrayList<>();
            for (float v : queryVectorArray) {
                queryVector.add(v);
            }
            log.info("Query vectorized: query={}, vectorSize={}",
                     request.getQuery(), queryVector.size());

            // 2. Delegate search to VectorStore
            List<VectorStore.SearchResult> searchResults = vectorStore.search(
                    queryVector, request.getTopK(), request.getThreshold());

            // 3. Apply category filter and convert to DocumentChunk
            List<RAGSearchResponse.DocumentChunk> results = searchResults.stream()
                    .filter(r -> request.getCategory() == null ||
                            request.getCategory().equals(r.category()))
                    .map(r -> RAGSearchResponse.DocumentChunk.builder()
                            .chunkId(r.id())
                            .docId(r.docId())
                            .docTitle(r.docTitle())
                            .content(r.content())
                            .score(r.score())
                            .category(r.category())
                            .chunkIndex(r.chunkIndex())
                            .build())
                    .collect(Collectors.toCollection(ArrayList::new));

            // 4. Sort by score descending
            results.sort((a, b) -> Double.compare(b.getScore(), a.getScore()));

            // 5. Build enhanced context
            String enhancedContext = buildEnhancedContext(results);

            return RAGSearchResponse.builder()
                    .query(request.getQuery())
                    .chunks(results)
                    .totalChunks(results.size())
                    .enhancedContext(enhancedContext)
                    .build();

        } catch (Exception e) {
            log.error("RAG search failed: query={}", request.getQuery(), e);
            throw new RuntimeException("RAG search failed: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public boolean deleteDocument(String docId) {
        try {
            // 1. Get all chunks for the document
            List<KnowledgeChunk> chunks = chunkMapper.selectList(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<KnowledgeChunk>()
                            .eq(KnowledgeChunk::getDocId, docId)
            );

            // 2. Delete vectors from VectorStore
            vectorStore.deleteByDocId(docId);

            // 3. Delete database records (cascade handles related records)
            documentMapper.delete(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<KnowledgeDocument>()
                            .eq(KnowledgeDocument::getDocId, docId)
            );
            chunkMapper.delete(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<KnowledgeChunk>()
                            .eq(KnowledgeChunk::getDocId, docId)
            );

            log.info("Document deleted: docId={}, chunkCount={}", docId, chunks.size());
            return true;

        } catch (Exception e) {
            log.error("Document deletion failed: docId={}", docId, e);
            return false;
        }
    }

    @Override
    public List<Object> listDocuments(String category) {
        try {
            return new ArrayList<>(documentMapper.selectList(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<KnowledgeDocument>()
                            .eq(category != null, KnowledgeDocument::getCategory, category)
                            .orderByDesc(KnowledgeDocument::getUploadTime)
            ));
        } catch (Exception e) {
            log.error("Failed to list documents: category={}", category, e);
            return new ArrayList<>();
        }
    }

    @Override
    public void initializeCollection() {
        if (vectorStore.isAvailable()) {
            log.info("Vector store is available and ready");
        } else {
            log.warn("Vector store is not available. Check Milvus connection.");
        }
    }

    /**
     * Build enhanced context from search results.
     */
    private String buildEnhancedContext(List<RAGSearchResponse.DocumentChunk> chunks) {
        if (chunks.isEmpty()) {
            return "No relevant knowledge base content found.";
        }

        StringBuilder context = new StringBuilder();
        context.append("Knowledge base search results:\n\n");

        for (int i = 0; i < chunks.size(); i++) {
            RAGSearchResponse.DocumentChunk chunk = chunks.get(i);
            context.append(String.format("[Source%d] %s (similarity: %.2f%%)\n",
                    i + 1, chunk.getDocTitle(), chunk.getScore() * 100));
            context.append(chunk.getContent()).append("\n\n");
        }

        return context.toString();
    }
}
