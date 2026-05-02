package com.travel.ai.dto;

import lombok.Data;

/**
 * Request DTO for updating a single user preference.
 * All fields are required; validation is performed in the controller
 * to ensure consistent error responses using the project's Result format.
 */
@Data
public class PreferenceUpdateRequest {
    private String userId;
    private String key;
    private String value;
    private Double confidence = 0.8;
}
