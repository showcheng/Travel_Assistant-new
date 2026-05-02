package com.travel.ai.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * User profile response DTO.
 * Contains profile metadata along with extracted preferences.
 */
@Data
@Builder
public class UserProfileResponse {
    private String userId;
    private String profileJson;
    private Map<String, String> preferences;
    private Integer sourceCount;
    private String lastConversationId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String message;
}
