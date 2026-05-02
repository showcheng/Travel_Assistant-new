package com.travel.ai.store;

import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory vector store implementation.
 * Used as a fallback when Milvus is unavailable.
 * Wraps the HashMap-based logic previously in KnowledgeBaseServiceSimpleImpl.
 *
 * Note: Vectors are lost on application restart. Use MilvusVectorStoreImpl for production.
 */
@Slf4j
public class InMemoryVectorStoreImpl implements VectorStore {

    private final Map<String, List<Float>> vectorStore = new ConcurrentHashMap<>();
    private final Map<String, ChunkMetadata> metadataStore = new ConcurrentHashMap<>();

    @Override
    public void upsert(String id, List<Float> vector, Map<String, String> metadata) {
        vectorStore.put(id, new ArrayList<>(vector));

        ChunkMetadata chunkMeta = new ChunkMetadata(
                id,
                metadata.getOrDefault("doc_id", ""),
                metadata.getOrDefault("doc_title", ""),
                metadata.getOrDefault("content", ""),
                metadata.getOrDefault("category", ""),
                parseChunkIndex(metadata.get("chunk_index"))
        );
        metadataStore.put(id, chunkMeta);
        log.debug("Upserted vector to memory: chunkId={}", id);
    }

    @Override
    public List<VectorStore.SearchResult> search(List<Float> queryVector,
                                                  int topK,
                                                  double threshold) {
        List<VectorStore.SearchResult> results = new ArrayList<>();

        for (Map.Entry<String, List<Float>> entry : vectorStore.entrySet()) {
            String chunkId = entry.getKey();
            List<Float> storedVector = entry.getValue();

            double similarity = cosineSimilarity(queryVector, storedVector);

            if (similarity >= threshold) {
                ChunkMetadata meta = metadataStore.get(chunkId);
                if (meta != null) {
                    results.add(new VectorStore.SearchResult(
                            meta.chunkId,
                            meta.docId,
                            meta.docTitle,
                            meta.content,
                            similarity,
                            meta.category,
                            meta.chunkIndex
                    ));
                }
            }
        }

        results.sort((a, b) -> Double.compare(b.score(), a.score()));
        if (results.size() > topK) {
            results = results.subList(0, topK);
        }

        return results;
    }

    @Override
    public void deleteByDocId(String docId) {
        List<String> toRemove = new ArrayList<>();
        for (Map.Entry<String, ChunkMetadata> entry : metadataStore.entrySet()) {
            if (docId.equals(entry.getValue().docId)) {
                toRemove.add(entry.getKey());
            }
        }
        for (String chunkId : toRemove) {
            vectorStore.remove(chunkId);
            metadataStore.remove(chunkId);
        }
        log.info("Deleted {} vectors for docId={}", toRemove.size(), docId);
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    /**
     * Get the number of stored vectors (for testing/monitoring).
     */
    public int size() {
        return vectorStore.size();
    }

    private double cosineSimilarity(List<Float> vector1, List<Float> vector2) {
        if (vector1.size() != vector2.size()) {
            throw new IllegalArgumentException("Vector dimension mismatch");
        }

        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;

        for (int i = 0; i < vector1.size(); i++) {
            dotProduct += vector1.get(i) * vector2.get(i);
            norm1 += vector1.get(i) * vector1.get(i);
            norm2 += vector2.get(i) * vector2.get(i);
        }

        if (norm1 == 0.0 || norm2 == 0.0) {
            return 0.0;
        }

        return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }

    private int parseChunkIndex(String value) {
        if (value == null) {
            return 0;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * Immutable metadata for a stored chunk.
     */
    private record ChunkMetadata(
            String chunkId,
            String docId,
            String docTitle,
            String content,
            String category,
            int chunkIndex
    ) {}
}
