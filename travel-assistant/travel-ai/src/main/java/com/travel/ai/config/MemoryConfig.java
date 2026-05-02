package com.travel.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Memory service configuration properties.
 *
 * Controls time-decay behavior, profile size limits, and scheduled cleanup
 * for the user profile memory system.
 *
 * Properties are bound from the "travel.memory" prefix in application.yml.
 *
 * Paper 5.3.4: User profile memory mechanism - configurable decay parameters
 */
@Configuration
@ConfigurationProperties(prefix = "travel.memory")
@Data
public class MemoryConfig {

    /**
     * Exponential decay rate (lambda) applied to preference confidence over time.
     * Formula: confidence * exp(-decayRate * daysSinceUpdate)
     * Higher values cause faster forgetting.
     */
    private double decayRate = 0.005;

    /**
     * Minimum confidence threshold. Preferences whose decayed confidence
     * falls below this value are removed during cleanup or profile load.
     */
    private double minConfidence = 0.1;

    /**
     * Maximum allowed profile JSON size in characters.
     * Profiles exceeding this limit are consolidated or truncated.
     */
    private int maxProfileSize = 2048;

    /**
     * Interval in minutes between scheduled cleanup runs.
     * The cleanup job scans all profiles and removes expired preferences.
     */
    private int cleanupIntervalMinutes = 60;

    /**
     * Minimum number of conversation messages required before
     * triggering LLM-based preference distillation.
     */
    private int distillationMinMessages = 4;
}
