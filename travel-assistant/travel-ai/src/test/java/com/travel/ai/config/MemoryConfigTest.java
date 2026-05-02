package com.travel.ai.config;

import com.travel.ai.config.BaseServiceTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for MemoryConfig.
 *
 * Tests verify that default values are correct, setters work, and
 * ConfigurationProperties binding produces expected values.
 */
class MemoryConfigTest extends BaseServiceTest {

    @Test
    @DisplayName("Default decayRate should be 0.005")
    void testDefaultDecayRate() {
        MemoryConfig config = new MemoryConfig();
        assertEquals(0.005, config.getDecayRate(), 0.0001,
                "Default decay rate should be 0.005");
    }

    @Test
    @DisplayName("Default minConfidence should be 0.1")
    void testDefaultMinConfidence() {
        MemoryConfig config = new MemoryConfig();
        assertEquals(0.1, config.getMinConfidence(), 0.0001,
                "Default min confidence should be 0.1");
    }

    @Test
    @DisplayName("Default maxProfileSize should be 2048")
    void testDefaultMaxProfileSize() {
        MemoryConfig config = new MemoryConfig();
        assertEquals(2048, config.getMaxProfileSize(),
                "Default max profile size should be 2048");
    }

    @Test
    @DisplayName("Default cleanupIntervalMinutes should be 60")
    void testDefaultCleanupIntervalMinutes() {
        MemoryConfig config = new MemoryConfig();
        assertEquals(60, config.getCleanupIntervalMinutes(),
                "Default cleanup interval should be 60 minutes");
    }

    @Test
    @DisplayName("Default distillationMinMessages should be 4")
    void testDefaultDistillationMinMessages() {
        MemoryConfig config = new MemoryConfig();
        assertEquals(4, config.getDistillationMinMessages(),
                "Default distillation min messages should be 4");
    }

    @Test
    @DisplayName("Setters should update values correctly")
    void testSetters() {
        MemoryConfig config = new MemoryConfig();

        config.setDecayRate(0.01);
        assertEquals(0.01, config.getDecayRate(), 0.0001);

        config.setMinConfidence(0.2);
        assertEquals(0.2, config.getMinConfidence(), 0.0001);

        config.setMaxProfileSize(4096);
        assertEquals(4096, config.getMaxProfileSize());

        config.setCleanupIntervalMinutes(120);
        assertEquals(120, config.getCleanupIntervalMinutes());

        config.setDistillationMinMessages(8);
        assertEquals(8, config.getDistillationMinMessages());
    }

    @Test
    @DisplayName("Config object should be independently mutable (immutability safety)")
    void testIndependentInstances() {
        MemoryConfig config1 = new MemoryConfig();
        MemoryConfig config2 = new MemoryConfig();

        config1.setDecayRate(0.02);
        assertEquals(0.005, config2.getDecayRate(), 0.0001,
                "Changing one instance should not affect another");
    }

    @Test
    @DisplayName("All defaults are non-null and within valid ranges")
    void testDefaultsAreValid() {
        MemoryConfig config = new MemoryConfig();

        assertTrue(config.getDecayRate() > 0,
                "Decay rate must be positive");
        assertTrue(config.getDecayRate() <= 1.0,
                "Decay rate should be reasonable (<=1.0)");

        assertTrue(config.getMinConfidence() > 0,
                "Min confidence must be positive");
        assertTrue(config.getMinConfidence() < 1.0,
                "Min confidence should be less than 1.0");

        assertTrue(config.getMaxProfileSize() > 0,
                "Max profile size must be positive");

        assertTrue(config.getCleanupIntervalMinutes() > 0,
                "Cleanup interval must be positive");

        assertTrue(config.getDistillationMinMessages() > 0,
                "Distillation min messages must be positive");
    }
}
