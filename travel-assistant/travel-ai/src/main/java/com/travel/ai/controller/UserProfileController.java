package com.travel.ai.controller;

import com.travel.ai.dto.PreferenceUpdateRequest;
import com.travel.ai.entity.UserProfile;
import com.travel.ai.service.UserProfileMemoryService;
import com.travel.common.response.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * REST controller for managing user profile memories.
 *
 * Provides endpoints to view, update, and manage user preferences
 * extracted from conversations. This enables the frontend to display
 * and control the personalization data the AI uses.
 *
 * Thesis 5.3.4: User profile memory mechanism
 */
@Slf4j
@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class UserProfileController {

    private final UserProfileMemoryService profileService;

    /**
     * Get user profile details including preferences and metadata.
     *
     * @param userId the user identifier
     * @return profile data wrapped in standard Result
     */
    @GetMapping("/{userId}")
    public Result<Map<String, Object>> getProfile(@PathVariable String userId) {
        try {
            log.debug("Getting profile for userId={}", userId);

            UserProfile profile = profileService.getProfile(userId);
            Map<String, String> preferences = profileService.getPreferences(userId);

            Map<String, Object> data = new HashMap<>();
            data.put("userId", userId);
            data.put("preferences", preferences);

            if (profile != null) {
                data.put("profileJson", profile.getProfileJson());
                data.put("sourceCount", profile.getSourceCount());
                data.put("lastConversationId", profile.getLastConversationId());
                data.put("createdAt", profile.getCreatedAt());
                data.put("updatedAt", profile.getUpdatedAt());
                return Result.success("Profile retrieved successfully", data);
            } else {
                data.put("profileJson", null);
                data.put("sourceCount", null);
                data.put("lastConversationId", null);
                data.put("createdAt", null);
                data.put("updatedAt", null);
                return Result.success("No profile found for this user", data);
            }
        } catch (Exception e) {
            log.error("Failed to get profile for userId={}", userId, e);
            return Result.error("Failed to get profile: " + e.getMessage());
        }
    }

    /**
     * Get user preferences as a simple key-value map.
     *
     * @param userId the user identifier
     * @return preference map wrapped in standard Result
     */
    @GetMapping("/{userId}/preferences")
    public Result<Map<String, String>> getPreferences(@PathVariable String userId) {
        try {
            log.debug("Getting preferences for userId={}", userId);
            Map<String, String> preferences = profileService.getPreferences(userId);
            return Result.success(preferences);
        } catch (Exception e) {
            log.error("Failed to get preferences for userId={}", userId, e);
            return Result.error("Failed to get preferences: " + e.getMessage());
        }
    }

    /**
     * Get formatted profile context for debugging.
     * This is the same context string that gets injected into LLM prompts.
     *
     * @param userId the user identifier
     * @return formatted context string wrapped in standard Result
     */
    @GetMapping("/{userId}/context")
    public Result<Map<String, Object>> getProfileContext(@PathVariable String userId) {
        try {
            log.debug("Getting profile context for userId={}", userId);
            String context = profileService.getProfileContext(userId);

            Map<String, Object> data = new HashMap<>();
            data.put("userId", userId);
            data.put("context", context);
            return Result.success(data);
        } catch (Exception e) {
            log.error("Failed to get profile context for userId={}", userId, e);
            return Result.error("Failed to get profile context: " + e.getMessage());
        }
    }

    /**
     * Update or add a single user preference.
     * Validates required fields before delegating to the service.
     *
     * @param request the preference update request
     * @return success or error result
     */
    @PostMapping("/preference")
    public Result<Void> updatePreference(@RequestBody PreferenceUpdateRequest request) {
        try {
            // Validate required fields
            if (request.getUserId() == null || request.getUserId().isBlank()) {
                return Result.error(400, "userId is required");
            }
            if (request.getKey() == null || request.getKey().isBlank()) {
                return Result.error(400, "preference key is required");
            }
            if (request.getValue() == null || request.getValue().isBlank()) {
                return Result.error(400, "preference value is required");
            }

            // Validate confidence range
            double confidence = request.getConfidence() != null ? request.getConfidence() : 0.8;
            if (confidence < 0.0 || confidence > 1.0) {
                return Result.error(400, "confidence must be between 0.0 and 1.0");
            }

            log.debug("Updating preference for userId={}, key={}, confidence={}",
                    request.getUserId(), request.getKey(), confidence);

            profileService.updatePreference(
                    request.getUserId(),
                    request.getKey(),
                    request.getValue(),
                    confidence
            );

            return Result.success();
        } catch (Exception e) {
            log.error("Failed to update preference for userId={}", request.getUserId(), e);
            return Result.error("Failed to update preference: " + e.getMessage());
        }
    }

    /**
     * Remove a single preference from the user profile.
     *
     * @param userId the user identifier
     * @param key    the preference key to remove
     * @return success or error result
     */
    @DeleteMapping("/{userId}/preference/{key}")
    public Result<Void> removePreference(
            @PathVariable String userId,
            @PathVariable String key) {
        try {
            log.debug("Removing preference for userId={}, key={}", userId, key);
            profileService.removePreference(userId, key);
            return Result.success();
        } catch (Exception e) {
            log.error("Failed to remove preference for userId={}, key={}", userId, key, e);
            return Result.error("Failed to remove preference: " + e.getMessage());
        }
    }

    /**
     * Enforce memory limit for a user profile.
     * Consolidates preferences when the profile exceeds the maximum size.
     *
     * @param userId the user identifier
     * @return success or error result
     */
    @PostMapping("/{userId}/compact")
    public Result<Void> compactMemory(@PathVariable String userId) {
        try {
            log.debug("Compacting memory for userId={}", userId);
            profileService.enforceMemoryLimit(userId);
            return Result.success();
        } catch (Exception e) {
            log.error("Failed to compact memory for userId={}", userId, e);
            return Result.error("Failed to compact memory: " + e.getMessage());
        }
    }

    /**
     * Health check endpoint for the user profile service.
     *
     * @return service health status
     */
    @GetMapping("/health")
    public Result<Map<String, Object>> health() {
        Map<String, Object> healthInfo = new HashMap<>();
        healthInfo.put("status", "UP");
        healthInfo.put("service", "user-profile-memory");
        healthInfo.put("timestamp", LocalDateTime.now());
        return Result.success(healthInfo);
    }
}
