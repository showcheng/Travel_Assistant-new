package com.travel.ai.service;

import com.travel.ai.enums.IntentType;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.Map;

/**
 * Dynamic threshold service for RAG vector search.
 *
 * Maps each IntentType to an appropriate similarity threshold,
 * controlling the precision/recall trade-off based on the nature
 * of the user's intent. Fact-based queries require higher thresholds
 * (more precise matches), while open-ended queries use lower thresholds
 * (broader recall).
 */
@Service
public class DynamicThresholdService {

    private static final double DEFAULT_THRESHOLD = 0.3;
    private static final int DEFAULT_TOP_K = 3;

    private static final Map<IntentType, Double> THRESHOLD_MAP;
    private static final Map<IntentType, Integer> TOP_K_MAP;

    static {
        // Threshold mapping: higher values = more precise, lower = broader recall
        THRESHOLD_MAP = new EnumMap<>(IntentType.class);
        THRESHOLD_MAP.put(IntentType.PRICE_INQUIRY, 0.5);
        THRESHOLD_MAP.put(IntentType.ATTRACTION_QUERY, 0.5);
        THRESHOLD_MAP.put(IntentType.PAYMENT_INQUIRY, 0.4);
        THRESHOLD_MAP.put(IntentType.POLICY_INQUIRY, 0.4);
        THRESHOLD_MAP.put(IntentType.PRODUCT_RECOMMENDATION, 0.3);
        THRESHOLD_MAP.put(IntentType.ORDER_QUERY, 0.4);
        THRESHOLD_MAP.put(IntentType.GENERAL, 0.2);
        THRESHOLD_MAP.put(IntentType.UNKNOWN, 0.3);

        // TopK mapping: fewer for precise queries, more for broad recall
        TOP_K_MAP = new EnumMap<>(IntentType.class);
        TOP_K_MAP.put(IntentType.PRICE_INQUIRY, 3);
        TOP_K_MAP.put(IntentType.ATTRACTION_QUERY, 3);
        TOP_K_MAP.put(IntentType.PAYMENT_INQUIRY, 3);
        TOP_K_MAP.put(IntentType.POLICY_INQUIRY, 3);
        TOP_K_MAP.put(IntentType.ORDER_QUERY, 3);
        TOP_K_MAP.put(IntentType.PRODUCT_RECOMMENDATION, 5);
        TOP_K_MAP.put(IntentType.GENERAL, 5);
        TOP_K_MAP.put(IntentType.UNKNOWN, 3);
    }

    /**
     * Returns the similarity threshold for the given intent type.
     *
     * @param intentType the classified intent type
     * @return the threshold value, or null if RAG search is not needed
     *         (e.g., GREETING intent)
     */
    public Double getThreshold(IntentType intentType) {
        if (intentType == IntentType.GREETING) {
            return null;
        }
        return THRESHOLD_MAP.getOrDefault(intentType, DEFAULT_THRESHOLD);
    }

    /**
     * Determines whether the given intent requires RAG vector search.
     *
     * @param intentType the classified intent type
     * @return true if RAG search should be performed, false otherwise
     */
    public boolean needsRAG(IntentType intentType) {
        return intentType != IntentType.GREETING && getThreshold(intentType) != null;
    }

    /**
     * Returns the number of results (topK) to retrieve from RAG search
     * for the given intent type.
     *
     * Fact-based queries use topK=3 (fewer, more precise results),
     * while recommendation and open-ended queries use topK=5
     * (more diverse results).
     *
     * @param intentType the classified intent type
     * @return the number of results to retrieve, or 0 if RAG is not needed
     */
    public int getTopK(IntentType intentType) {
        if (intentType == IntentType.GREETING) {
            return 0;
        }
        return TOP_K_MAP.getOrDefault(intentType, DEFAULT_TOP_K);
    }
}
