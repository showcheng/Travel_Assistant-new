package com.travel.ai.service;

import com.travel.ai.config.BaseServiceTest;
import com.travel.ai.enums.IntentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DynamicThresholdService unit tests.
 *
 * Validates that each IntentType maps to the correct similarity threshold
 * for RAG vector search, per the thesis specification.
 */
class DynamicThresholdServiceTest extends BaseServiceTest {

    private final DynamicThresholdService service = new DynamicThresholdService();

    // ========== Threshold mapping tests ==========

    @Test
    @DisplayName("PRICE_INQUIRY should map to threshold 0.5 (fact-based, high precision)")
    void testFactQueryThreshold_PriceInquiry() {
        Double threshold = service.getThreshold(IntentType.PRICE_INQUIRY);
        assertNotNull(threshold);
        assertEquals(0.5, threshold, 0.001);
    }

    @Test
    @DisplayName("ATTRACTION_QUERY should map to threshold 0.5 (fact-based)")
    void testFactQueryThreshold_AttractionQuery() {
        Double threshold = service.getThreshold(IntentType.ATTRACTION_QUERY);
        assertNotNull(threshold);
        assertEquals(0.5, threshold, 0.001);
    }

    @Test
    @DisplayName("PRODUCT_RECOMMENDATION should map to threshold 0.3 (recommendation, balanced)")
    void testRecommendQueryThreshold_ProductRecommendation() {
        Double threshold = service.getThreshold(IntentType.PRODUCT_RECOMMENDATION);
        assertNotNull(threshold);
        assertEquals(0.3, threshold, 0.001);
    }

    @Test
    @DisplayName("GENERAL should map to threshold 0.2 (open-ended, high recall)")
    void testOpenQueryThreshold_General() {
        Double threshold = service.getThreshold(IntentType.GENERAL);
        assertNotNull(threshold);
        assertEquals(0.2, threshold, 0.001);
    }

    @Test
    @DisplayName("GREETING should return null threshold (no RAG needed)")
    void testGreetingNoThreshold() {
        Double threshold = service.getThreshold(IntentType.GREETING);
        assertNull(threshold);
    }

    @Test
    @DisplayName("UNKNOWN should map to default threshold 0.3 (fallback)")
    void testUnknownIntentDefault() {
        Double threshold = service.getThreshold(IntentType.UNKNOWN);
        assertNotNull(threshold);
        assertEquals(0.3, threshold, 0.001);
    }

    // ========== Completeness tests ==========

    @Test
    @DisplayName("Every IntentType enum value should have a threshold mapping or be explicitly excluded")
    void testAllIntentTypesHaveMapping() {
        for (IntentType intentType : IntentType.values()) {
            if (intentType == IntentType.GREETING) {
                // GREETING is explicitly excluded from RAG
                assertNull(service.getThreshold(intentType),
                        "GREETING should have null threshold (no RAG)");
            } else {
                assertNotNull(service.getThreshold(intentType),
                        "IntentType " + intentType + " should have a non-null threshold");
            }
        }
    }

    @Test
    @DisplayName("All non-null thresholds must be in valid range [0.0, 1.0]")
    void testThresholdRange() {
        for (IntentType intentType : IntentType.values()) {
            Double threshold = service.getThreshold(intentType);
            if (threshold != null) {
                assertTrue(threshold >= 0.0 && threshold <= 1.0,
                        "Threshold for " + intentType + " (" + threshold + ") must be in [0.0, 1.0]");
            }
        }
    }

    // ========== needsRAG tests ==========

    @Test
    @DisplayName("GREETING should not need RAG search")
    void testNeedsRAG_Greeting() {
        assertFalse(service.needsRAG(IntentType.GREETING));
    }

    @Test
    @DisplayName("PRICE_INQUIRY should need RAG search")
    void testNeedsRAG_FactQuery() {
        assertTrue(service.needsRAG(IntentType.PRICE_INQUIRY));
    }

    // ========== TopK mapping tests ==========

    @Test
    @DisplayName("Fact queries (PRICE_INQUIRY) should return topK=3 (fewer, more precise)")
    void testTopK_FactQuery() {
        int topK = service.getTopK(IntentType.PRICE_INQUIRY);
        assertEquals(3, topK);
    }

    @Test
    @DisplayName("Recommendation queries (PRODUCT_RECOMMENDATION) should return topK=5 (more diverse)")
    void testTopK_RecommendationQuery() {
        int topK = service.getTopK(IntentType.PRODUCT_RECOMMENDATION);
        assertEquals(5, topK);
    }

    @Test
    @DisplayName("Open queries (GENERAL) should return topK=5")
    void testTopK_OpenQuery() {
        int topK = service.getTopK(IntentType.GENERAL);
        assertEquals(5, topK);
    }

    @Test
    @DisplayName("GREETING should return topK=0 (no RAG search)")
    void testTopK_Greeting() {
        int topK = service.getTopK(IntentType.GREETING);
        assertEquals(0, topK);
    }

    @Test
    @DisplayName("Every IntentType should have a valid topK >= 0")
    void testAllIntentTypesHaveTopK() {
        for (IntentType intentType : IntentType.values()) {
            int topK = service.getTopK(intentType);
            assertTrue(topK >= 0,
                    "TopK for " + intentType + " (" + topK + ") must be >= 0");
        }
    }
}
