package com.travel.ai.store;

import com.travel.ai.config.BaseServiceTest;
import io.milvus.v2.client.MilvusClientV2;
import io.milvus.v2.service.vector.request.DeleteReq;
import io.milvus.v2.service.vector.request.SearchReq;
import io.milvus.v2.service.vector.request.UpsertReq;
import io.milvus.v2.service.vector.response.SearchResp;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for MilvusVectorStoreImpl.
 * All Milvus interactions are mocked - no real Milvus connection needed.
 */
class MilvusVectorStoreTest extends BaseServiceTest {

    @Mock
    private MilvusClientV2 milvusClient;

    private MilvusVectorStoreImpl vectorStore;

    private static final String COLLECTION_NAME = "travel_knowledge";
    private static final int DIMENSION = 768;

    @BeforeEach
    void setUp() {
        vectorStore = new MilvusVectorStoreImpl(milvusClient, COLLECTION_NAME, DIMENSION);
    }

    /**
     * Test 1: upsert vectors - verify upsert called with correct params
     */
    @Test
    void testUpsert_CalledWithCorrectParams() {
        // Arrange
        String id = "chunk-001";
        List<Float> vector = createTestVector(DIMENSION, 0.5f);
        Map<String, String> metadata = new HashMap<>();
        metadata.put("doc_id", "doc-123");
        metadata.put("doc_title", "Test Document");
        metadata.put("content", "This is test content");
        metadata.put("category", "scenic");
        metadata.put("chunk_index", "0");

        // Act
        vectorStore.upsert(id, vector, metadata);

        // Assert - verify milvusClient.upsert() was called with UpsertReq
        ArgumentCaptor<UpsertReq> captor = ArgumentCaptor.forClass(UpsertReq.class);
        verify(milvusClient).upsert(captor.capture());

        UpsertReq request = captor.getValue();
        assertNotNull(request, "UpsertReq should not be null");
        assertEquals(COLLECTION_NAME, request.getCollectionName(),
                "Collection name should match");
        assertNotNull(request.getData(), "Upsert data should not be null");
        assertEquals(1, request.getData().size(), "Should have 1 data row");
    }

    /**
     * Test 2: search vectors - verify search returns results in correct format
     */
    @Test
    void testSearch_TopK_ReturnsResults() {
        // Arrange
        List<Float> queryVector = createTestVector(DIMENSION, 0.8f);

        Map<String, Object> entity1 = new HashMap<>();
        entity1.put("chunk_id", "chunk-001");
        entity1.put("doc_id", "doc-123");
        entity1.put("doc_title", "Test Doc 1");
        entity1.put("content", "Content 1");
        entity1.put("category", "scenic");
        entity1.put("chunk_index", "0");

        SearchResp.SearchResult result1 = SearchResp.SearchResult.builder()
                .id(1L)
                .distance(0.95f)
                .entity(entity1)
                .build();

        Map<String, Object> entity2 = new HashMap<>();
        entity2.put("chunk_id", "chunk-002");
        entity2.put("doc_id", "doc-456");
        entity2.put("doc_title", "Test Doc 2");
        entity2.put("content", "Content 2");
        entity2.put("category", "food");
        entity2.put("chunk_index", "1");

        SearchResp.SearchResult result2 = SearchResp.SearchResult.builder()
                .id(2L)
                .distance(0.85f)
                .entity(entity2)
                .build();

        // SearchResp.getSearchResults() returns List<List<SearchResult>>
        SearchResp searchResp = SearchResp.builder()
                .searchResults(List.of(List.of(result1, result2)))
                .build();

        when(milvusClient.search(any(SearchReq.class))).thenReturn(searchResp);

        // Act
        List<VectorStore.SearchResult> results = vectorStore.search(queryVector, 5, 0.5);

        // Assert
        assertEquals(2, results.size(), "Should return 2 results");

        VectorStore.SearchResult first = results.get(0);
        assertEquals("chunk-001", first.id());
        assertEquals("doc-123", first.docId());
        assertEquals("Test Doc 1", first.docTitle());
        assertEquals("Content 1", first.content());
        assertEquals("scenic", first.category());
        assertEquals(0, first.chunkIndex());
        assertTrue(first.score() >= 0.5, "Score should meet threshold");

        verify(milvusClient).search(any(SearchReq.class));
    }

    /**
     * Test 3: search with threshold - low score results filtered out
     */
    @Test
    void testSearch_ThresholdFilter() {
        // Arrange
        List<Float> queryVector = createTestVector(DIMENSION, 0.8f);

        // This result has a high score (0.9) - should pass threshold
        Map<String, Object> entity1 = new HashMap<>();
        entity1.put("chunk_id", "chunk-001");
        entity1.put("doc_id", "doc-123");
        entity1.put("doc_title", "Good Match");
        entity1.put("content", "High relevance content");
        entity1.put("category", "scenic");
        entity1.put("chunk_index", "0");

        SearchResp.SearchResult highScoreResult = SearchResp.SearchResult.builder()
                .id(1L)
                .distance(0.9f)
                .entity(entity1)
                .build();

        // This result has a low score (0.3) - should be filtered out by threshold
        Map<String, Object> entity2 = new HashMap<>();
        entity2.put("chunk_id", "chunk-002");
        entity2.put("doc_id", "doc-456");
        entity2.put("doc_title", "Poor Match");
        entity2.put("content", "Low relevance content");
        entity2.put("category", "food");
        entity2.put("chunk_index", "1");

        SearchResp.SearchResult lowScoreResult = SearchResp.SearchResult.builder()
                .id(2L)
                .distance(0.3f)
                .entity(entity2)
                .build();

        SearchResp searchResp = SearchResp.builder()
                .searchResults(List.of(List.of(highScoreResult, lowScoreResult)))
                .build();

        when(milvusClient.search(any(SearchReq.class))).thenReturn(searchResp);

        // Act - threshold of 0.5 should filter out the 0.3 score result
        List<VectorStore.SearchResult> results = vectorStore.search(queryVector, 5, 0.5);

        // Assert - only the high score result should remain
        assertEquals(1, results.size(), "Low score results should be filtered out");
        assertEquals("chunk-001", results.get(0).id());
    }

    /**
     * Test 4: delete vectors by doc ID
     */
    @Test
    void testDelete_ByDocId() {
        // Arrange
        String docId = "doc-123";

        // Act
        vectorStore.deleteByDocId(docId);

        // Assert - verify delete was called with DeleteReq containing doc_id filter
        ArgumentCaptor<DeleteReq> captor = ArgumentCaptor.forClass(DeleteReq.class);
        verify(milvusClient).delete(captor.capture());

        DeleteReq request = captor.getValue();
        assertNotNull(request, "DeleteReq should not be null");
        assertEquals(COLLECTION_NAME, request.getCollectionName(),
                "Collection name should match");
        assertNotNull(request.getFilter(), "Filter expression should not be null");
        assertTrue(request.getFilter().contains(docId),
                "Filter should contain the doc ID");
    }

    /**
     * Test 5: connection failure falls back gracefully
     */
    @Test
    void testSearch_ConnectionFailure_ReturnsEmpty() {
        // Arrange - simulate Milvus connection failure
        List<Float> queryVector = createTestVector(DIMENSION, 0.8f);

        when(milvusClient.search(any(SearchReq.class)))
                .thenThrow(new RuntimeException("Milvus connection refused"));

        // Act
        List<VectorStore.SearchResult> results = vectorStore.search(queryVector, 5, 0.5);

        // Assert - should return empty list, not throw exception
        assertNotNull(results, "Should return non-null list on failure");
        assertTrue(results.isEmpty(), "Should return empty list on connection failure");
    }

    /**
     * Test 6: full upload-search-delete cycle
     */
    @Test
    void testUploadAndSearch_FullCycle() {
        // Arrange - upload a document
        String chunkId = "chunk-cycle-001";
        List<Float> uploadVector = createTestVector(DIMENSION, 0.5f);
        Map<String, String> metadata = new HashMap<>();
        metadata.put("doc_id", "doc-cycle");
        metadata.put("doc_title", "Cycle Test Doc");
        metadata.put("content", "Cycle test content");
        metadata.put("category", "scenic");
        metadata.put("chunk_index", "0");

        // Act - upload
        vectorStore.upsert(chunkId, uploadVector, metadata);
        verify(milvusClient).upsert(any(UpsertReq.class));

        // Arrange - search setup
        List<Float> queryVector = createTestVector(DIMENSION, 0.5f);

        Map<String, Object> entity = new HashMap<>();
        entity.put("chunk_id", chunkId);
        entity.put("doc_id", "doc-cycle");
        entity.put("doc_title", "Cycle Test Doc");
        entity.put("content", "Cycle test content");
        entity.put("category", "scenic");
        entity.put("chunk_index", "0");

        SearchResp.SearchResult searchResult = SearchResp.SearchResult.builder()
                .id(1L)
                .distance(0.95f)
                .entity(entity)
                .build();

        SearchResp searchResp = SearchResp.builder()
                .searchResults(List.of(List.of(searchResult)))
                .build();
        when(milvusClient.search(any(SearchReq.class))).thenReturn(searchResp);

        // Act - search
        List<VectorStore.SearchResult> results = vectorStore.search(queryVector, 5, 0.5);

        // Assert - verify round trip
        assertEquals(1, results.size());
        assertEquals(chunkId, results.get(0).id());
        assertEquals("doc-cycle", results.get(0).docId());
        assertEquals("Cycle Test Doc", results.get(0).docTitle());
        assertEquals("Cycle test content", results.get(0).content());

        // Act - delete
        vectorStore.deleteByDocId("doc-cycle");
        verify(milvusClient).delete(any(DeleteReq.class));

        // Verify total interactions
        verify(milvusClient, times(1)).upsert(any());
        verify(milvusClient, times(1)).search(any());
        verify(milvusClient, times(1)).delete(any());
    }

    // Helper to create a test vector with consistent values
    private List<Float> createTestVector(int dimension, float fillValue) {
        List<Float> vector = new ArrayList<>(dimension);
        for (int i = 0; i < dimension; i++) {
            vector.add(fillValue + (i * 0.001f));
        }
        return vector;
    }
}
