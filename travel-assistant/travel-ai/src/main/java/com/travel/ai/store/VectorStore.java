package com.travel.ai.store;

import java.util.List;
import java.util.Map;

/**
 * Abstraction for vector storage operations.
 * Allows swapping between Milvus and in-memory implementations.
 */
public interface VectorStore {

    /**
     * Insert or update a vector with associated metadata.
     *
     * @param id       unique identifier for the vector (typically chunk ID)
     * @param vector   the embedding vector
     * @param metadata key-value metadata (doc_id, doc_title, content, category, chunk_index)
     */
    void upsert(String id, List<Float> vector, Map<String, String> metadata);

    /**
     * Search for similar vectors.
     *
     * @param queryVector the query embedding vector
     * @param topK        maximum number of results to return
     * @param threshold   minimum similarity score (0.0 - 1.0)
     * @return list of search results sorted by score descending
     */
    List<SearchResult> search(List<Float> queryVector, int topK, double threshold);

    /**
     * Delete all vectors belonging to a document.
     *
     * @param docId the document ID whose vectors should be removed
     */
    void deleteByDocId(String docId);

    /**
     * Check if the vector store is available and healthy.
     *
     * @return true if the store is operational
     */
    boolean isAvailable();

    /**
     * Immutable search result record.
     */
    record SearchResult(
            String id,
            String docId,
            String docTitle,
            String content,
            double score,
            String category,
            int chunkIndex
    ) {}
}
