package com.travel.ai.store;

import com.alibaba.fastjson.JSONObject;
import io.milvus.v2.client.MilvusClientV2;
import io.milvus.v2.service.vector.request.DeleteReq;
import io.milvus.v2.service.vector.request.SearchReq;
import io.milvus.v2.service.vector.request.UpsertReq;
import io.milvus.v2.service.vector.response.SearchResp;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

/**
 * Milvus-based vector store implementation.
 * Delegates all vector operations to a Milvus instance via the v2 SDK.
 */
@Slf4j
public class MilvusVectorStoreImpl implements VectorStore {

    private static final String VECTOR_FIELD = "vector";
    private static final String FIELD_CHUNK_ID = "chunk_id";
    private static final String FIELD_DOC_ID = "doc_id";
    private static final String FIELD_DOC_TITLE = "doc_title";
    private static final String FIELD_CONTENT = "content";
    private static final String FIELD_CATEGORY = "category";
    private static final String FIELD_CHUNK_INDEX = "chunk_index";

    private static final List<String> OUTPUT_FIELDS = List.of(
            FIELD_CHUNK_ID, FIELD_DOC_ID, FIELD_DOC_TITLE,
            FIELD_CONTENT, FIELD_CATEGORY, FIELD_CHUNK_INDEX
    );

    private final MilvusClientV2 milvusClient;
    private final String collectionName;
    private final int dimension;

    public MilvusVectorStoreImpl(MilvusClientV2 milvusClient,
                                  String collectionName,
                                  int dimension) {
        this.milvusClient = milvusClient;
        this.collectionName = collectionName;
        this.dimension = dimension;
    }

    @Override
    public void upsert(String id, List<Float> vector, Map<String, String> metadata) {
        try {
            JSONObject row = new JSONObject();
            row.put(FIELD_CHUNK_ID, id);
            row.put(VECTOR_FIELD, vector);
            row.put(FIELD_DOC_ID, metadata.getOrDefault(FIELD_DOC_ID, ""));
            row.put(FIELD_DOC_TITLE, metadata.getOrDefault(FIELD_DOC_TITLE, ""));
            row.put(FIELD_CONTENT, metadata.getOrDefault(FIELD_CONTENT, ""));
            row.put(FIELD_CATEGORY, metadata.getOrDefault(FIELD_CATEGORY, ""));
            row.put(FIELD_CHUNK_INDEX,
                    Integer.parseInt(metadata.getOrDefault(FIELD_CHUNK_INDEX, "0")));

            UpsertReq request = UpsertReq.builder()
                    .collectionName(collectionName)
                    .data(List.of(row))
                    .build();

            milvusClient.upsert(request);
            log.debug("Upserted vector: chunkId={}, collection={}", id, collectionName);
        } catch (Exception e) {
            log.error("Failed to upsert vector: chunkId={}", id, e);
            throw new RuntimeException("Vector upsert failed", e);
        }
    }

    @Override
    public List<VectorStore.SearchResult> search(List<Float> queryVector,
                                                  int topK,
                                                  double threshold) {
        try {
            SearchReq searchReq = SearchReq.builder()
                    .collectionName(collectionName)
                    .data(List.of(queryVector))
                    .topK(topK)
                    .outputFields(OUTPUT_FIELDS)
                    .searchParams(Map.of("metric_type", "COSINE"))
                    .build();

            SearchResp response = milvusClient.search(searchReq);

            List<VectorStore.SearchResult> results = new ArrayList<>();

            // SearchResp.getSearchResults() returns List<List<SearchResult>>
            for (List<SearchResp.SearchResult> innerList : response.getSearchResults()) {
                for (SearchResp.SearchResult hit : innerList) {
                    float distance = hit.getDistance() != null ? hit.getDistance() : 0.0f;

                    if (distance < threshold) {
                        continue;
                    }

                    Map<String, Object> entity = hit.getEntity();
                    if (entity == null) {
                        continue;
                    }

                    String chunkId = String.valueOf(entity.getOrDefault(FIELD_CHUNK_ID, ""));
                    String docId = String.valueOf(entity.getOrDefault(FIELD_DOC_ID, ""));
                    String docTitle = String.valueOf(entity.getOrDefault(FIELD_DOC_TITLE, ""));
                    String content = String.valueOf(entity.getOrDefault(FIELD_CONTENT, ""));
                    String category = String.valueOf(entity.getOrDefault(FIELD_CATEGORY, ""));
                    int chunkIndex = parseChunkIndex(entity.get(FIELD_CHUNK_INDEX));

                    results.add(new VectorStore.SearchResult(
                            chunkId, docId, docTitle, content,
                            distance, category, chunkIndex
                    ));
                }
            }

            // Sort by score descending
            results.sort((a, b) -> Double.compare(b.score(), a.score()));
            return results;

        } catch (Exception e) {
            log.error("Vector search failed", e);
            return Collections.emptyList();
        }
    }

    @Override
    public void deleteByDocId(String docId) {
        try {
            String filter = FIELD_DOC_ID + " == \"" + docId + "\"";

            DeleteReq request = DeleteReq.builder()
                    .collectionName(collectionName)
                    .filter(filter)
                    .build();

            milvusClient.delete(request);
            log.info("Deleted vectors for docId={}", docId);
        } catch (Exception e) {
            log.error("Failed to delete vectors for docId={}", docId, e);
            throw new RuntimeException("Vector delete failed", e);
        }
    }

    @Override
    public boolean isAvailable() {
        try {
            milvusClient.listCollections();
            return true;
        } catch (Exception e) {
            log.warn("Milvus availability check failed: {}", e.getMessage());
            return false;
        }
    }

    private int parseChunkIndex(Object value) {
        if (value == null) {
            return 0;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
