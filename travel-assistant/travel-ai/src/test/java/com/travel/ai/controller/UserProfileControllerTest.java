package com.travel.ai.controller;

import com.travel.ai.config.BaseServiceTest;
import com.travel.ai.dto.PreferenceUpdateRequest;
import com.travel.ai.entity.UserProfile;
import com.travel.ai.service.UserProfileMemoryService;
import com.travel.common.response.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * UserProfileController unit tests - TDD RED phase.
 * Tests the REST API endpoints for managing user profile memories.
 * Uses BaseServiceTest (MockitoExtension) consistent with project conventions.
 */
class UserProfileControllerTest extends BaseServiceTest {

    @Mock
    private UserProfileMemoryService profileService;

    @InjectMocks
    private UserProfileController controller;

    private UserProfile sampleProfile;
    private static final String USER_ID = "user-001";

    @BeforeEach
    void setUp() {
        sampleProfile = new UserProfile();
        sampleProfile.setId(1L);
        sampleProfile.setUserId(USER_ID);
        sampleProfile.setProfileJson("{\"preferences\":[{\"key\":\"travel_style\",\"value\":\"natural\"}]}");
        sampleProfile.setSourceCount(5);
        sampleProfile.setLastConversationId("conv-100");
        sampleProfile.setCreatedAt(LocalDateTime.of(2026, 1, 1, 10, 0));
        sampleProfile.setUpdatedAt(LocalDateTime.of(2026, 5, 1, 14, 30));
        sampleProfile.setVersion(3);
    }

    // =====================================================================
    // Test 1: GET /api/profile/{userId} -- returns profile data
    // =====================================================================
    @Test
    void testGetProfile_ReturnsProfileData() {
        // Arrange
        when(profileService.getProfile(USER_ID)).thenReturn(sampleProfile);
        when(profileService.getPreferences(USER_ID)).thenReturn(Map.of("travel_style", "natural"));

        // Act
        Result<Map<String, Object>> result = controller.getProfile(USER_ID);

        // Assert
        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertNotNull(result.getData());

        Map<String, Object> data = result.getData();
        assertEquals(USER_ID, data.get("userId"));
        assertEquals(sampleProfile.getProfileJson(), data.get("profileJson"));
        assertEquals(5, data.get("sourceCount"));
        assertEquals("conv-100", data.get("lastConversationId"));
        assertNotNull(data.get("preferences"));

        verify(profileService).getProfile(USER_ID);
        verify(profileService).getPreferences(USER_ID);
    }

    // =====================================================================
    // Test 2: GET /api/profile/{userId} -- unknown user returns message
    // =====================================================================
    @Test
    void testGetProfile_UnknownUser_ReturnsMessage() {
        // Arrange
        when(profileService.getProfile("unknown")).thenReturn(null);
        when(profileService.getPreferences("unknown")).thenReturn(Collections.emptyMap());

        // Act
        Result<Map<String, Object>> result = controller.getProfile("unknown");

        // Assert
        assertNotNull(result);
        assertTrue(result.isSuccess(), "Should return success even for unknown user");
        assertNotNull(result.getData());
        assertEquals("unknown", result.getData().get("userId"));
        assertNotNull(result.getMessage(), "Should include a descriptive message");
    }

    // =====================================================================
    // Test 3: GET /api/profile/{userId}/preferences -- returns preference map
    // =====================================================================
    @Test
    void testGetPreferences_ReturnsMap() {
        // Arrange
        Map<String, String> prefs = new LinkedHashMap<>();
        prefs.put("travel_style", "natural");
        prefs.put("budget_range", "3000-5000");
        when(profileService.getPreferences(USER_ID)).thenReturn(prefs);

        // Act
        Result<Map<String, String>> result = controller.getPreferences(USER_ID);

        // Assert
        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        assertEquals(2, result.getData().size());
        assertEquals("natural", result.getData().get("travel_style"));
        assertEquals("3000-5000", result.getData().get("budget_range"));

        verify(profileService).getPreferences(USER_ID);
    }

    // =====================================================================
    // Test 4: GET /api/profile/{userId}/preferences -- empty map for no profile
    // =====================================================================
    @Test
    void testGetPreferences_NoProfile_ReturnsEmptyMap() {
        // Arrange
        when(profileService.getPreferences("noprofile")).thenReturn(Collections.emptyMap());

        // Act
        Result<Map<String, String>> result = controller.getPreferences("noprofile");

        // Assert
        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        assertTrue(result.getData().isEmpty());
    }

    // =====================================================================
    // Test 5: GET /api/profile/{userId}/context -- returns formatted context
    // =====================================================================
    @Test
    void testGetProfileContext_ReturnsFormattedString() {
        // Arrange
        String context = "User prefers natural scenery, budget 3000-5000, travels with family.";
        when(profileService.getProfileContext(USER_ID)).thenReturn(context);

        // Act
        Result<Map<String, Object>> result = controller.getProfileContext(USER_ID);

        // Assert
        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        assertEquals(USER_ID, result.getData().get("userId"));
        assertEquals(context, result.getData().get("context"));

        verify(profileService).getProfileContext(USER_ID);
    }

    // =====================================================================
    // Test 6: POST /api/profile/preference -- updates successfully
    // =====================================================================
    @Test
    void testUpdatePreference_Success() {
        // Arrange
        PreferenceUpdateRequest request = new PreferenceUpdateRequest();
        request.setUserId(USER_ID);
        request.setKey("travel_style");
        request.setValue("adventure");
        request.setConfidence(0.9);

        // Act
        Result<Void> result = controller.updatePreference(request);

        // Assert
        assertNotNull(result);
        assertTrue(result.isSuccess());

        verify(profileService).updatePreference(USER_ID, "travel_style", "adventure", 0.9);
    }

    // =====================================================================
    // Test 7: POST /api/profile/preference -- uses default confidence
    // =====================================================================
    @Test
    void testUpdatePreference_DefaultConfidence() {
        // Arrange
        PreferenceUpdateRequest request = new PreferenceUpdateRequest();
        request.setUserId(USER_ID);
        request.setKey("dietary");
        request.setValue("vegetarian");
        // confidence not set, should default to 0.8

        // Act
        Result<Void> result = controller.updatePreference(request);

        // Assert
        assertTrue(result.isSuccess());
        verify(profileService).updatePreference(USER_ID, "dietary", "vegetarian", 0.8);
    }

    // =====================================================================
    // Test 8: POST /api/profile/preference -- missing userId returns error
    // =====================================================================
    @Test
    void testUpdatePreference_MissingUserId_ReturnsError() {
        // Arrange
        PreferenceUpdateRequest request = new PreferenceUpdateRequest();
        request.setUserId(null);
        request.setKey("travel_style");
        request.setValue("adventure");
        request.setConfidence(0.9);

        // Act
        Result<Void> result = controller.updatePreference(request);

        // Assert
        assertNotNull(result);
        assertFalse(result.isSuccess(), "Should fail when userId is missing");

        verify(profileService, never()).updatePreference(anyString(), anyString(), anyString(), anyDouble());
    }

    // =====================================================================
    // Test 9: POST /api/profile/preference -- missing key returns error
    // =====================================================================
    @Test
    void testUpdatePreference_MissingKey_ReturnsError() {
        // Arrange
        PreferenceUpdateRequest request = new PreferenceUpdateRequest();
        request.setUserId(USER_ID);
        request.setKey(null);
        request.setValue("adventure");
        request.setConfidence(0.9);

        // Act
        Result<Void> result = controller.updatePreference(request);

        // Assert
        assertFalse(result.isSuccess(), "Should fail when key is missing");

        verify(profileService, never()).updatePreference(anyString(), anyString(), anyString(), anyDouble());
    }

    // =====================================================================
    // Test 10: POST /api/profile/preference -- missing value returns error
    // =====================================================================
    @Test
    void testUpdatePreference_MissingValue_ReturnsError() {
        // Arrange
        PreferenceUpdateRequest request = new PreferenceUpdateRequest();
        request.setUserId(USER_ID);
        request.setKey("travel_style");
        request.setValue(null);
        request.setConfidence(0.9);

        // Act
        Result<Void> result = controller.updatePreference(request);

        // Assert
        assertFalse(result.isSuccess(), "Should fail when value is missing");

        verify(profileService, never()).updatePreference(anyString(), anyString(), anyString(), anyDouble());
    }

    // =====================================================================
    // Test 11: DELETE /api/profile/{userId}/preference/{key} -- removes pref
    // =====================================================================
    @Test
    void testRemovePreference_Success() {
        // Arrange
        doNothing().when(profileService).removePreference(USER_ID, "budget_range");

        // Act
        Result<Void> result = controller.removePreference(USER_ID, "budget_range");

        // Assert
        assertNotNull(result);
        assertTrue(result.isSuccess());

        verify(profileService).removePreference(USER_ID, "budget_range");
    }

    // =====================================================================
    // Test 12: DELETE /api/profile/{userId}/preference/{key} -- service error
    // =====================================================================
    @Test
    void testRemovePreference_ServiceError_ReturnsError() {
        // Arrange
        doThrow(new RuntimeException("Database error"))
                .when(profileService).removePreference(USER_ID, "bad_key");

        // Act
        Result<Void> result = controller.removePreference(USER_ID, "bad_key");

        // Assert
        assertNotNull(result);
        assertFalse(result.isSuccess(), "Should return error when service throws");
    }

    // =====================================================================
    // Test 13: POST /api/profile/{userId}/compact -- compacts memory
    // =====================================================================
    @Test
    void testCompactMemory_Success() {
        // Arrange
        doNothing().when(profileService).enforceMemoryLimit(USER_ID);

        // Act
        Result<Void> result = controller.compactMemory(USER_ID);

        // Assert
        assertNotNull(result);
        assertTrue(result.isSuccess());

        verify(profileService).enforceMemoryLimit(USER_ID);
    }

    // =====================================================================
    // Test 14: POST /api/profile/{userId}/compact -- service error
    // =====================================================================
    @Test
    void testCompactMemory_ServiceError_ReturnsError() {
        // Arrange
        doThrow(new RuntimeException("LLM consolidation failed"))
                .when(profileService).enforceMemoryLimit(USER_ID);

        // Act
        Result<Void> result = controller.compactMemory(USER_ID);

        // Assert
        assertNotNull(result);
        assertFalse(result.isSuccess(), "Should return error when compact fails");
    }

    // =====================================================================
    // Test 15: GET /api/profile/health -- returns health status
    // =====================================================================
    @Test
    void testHealth_ReturnsStatus() {
        // Act
        Result<Map<String, Object>> result = controller.health();

        // Assert
        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        assertEquals("UP", result.getData().get("status"));
        assertEquals("user-profile-memory", result.getData().get("service"));
    }

    // =====================================================================
    // Test 16: POST /api/profile/preference -- confidence out of range high
    // =====================================================================
    @Test
    void testUpdatePreference_ConfidenceTooHigh_ReturnsError() {
        // Arrange
        PreferenceUpdateRequest request = new PreferenceUpdateRequest();
        request.setUserId(USER_ID);
        request.setKey("travel_style");
        request.setValue("adventure");
        request.setConfidence(1.5);

        // Act
        Result<Void> result = controller.updatePreference(request);

        // Assert
        assertFalse(result.isSuccess(), "Should fail when confidence > 1.0");

        verify(profileService, never()).updatePreference(anyString(), anyString(), anyString(), anyDouble());
    }

    // =====================================================================
    // Test 17: POST /api/profile/preference -- confidence negative
    // =====================================================================
    @Test
    void testUpdatePreference_ConfidenceNegative_ReturnsError() {
        // Arrange
        PreferenceUpdateRequest request = new PreferenceUpdateRequest();
        request.setUserId(USER_ID);
        request.setKey("travel_style");
        request.setValue("adventure");
        request.setConfidence(-0.1);

        // Act
        Result<Void> result = controller.updatePreference(request);

        // Assert
        assertFalse(result.isSuccess(), "Should fail when confidence is negative");

        verify(profileService, never()).updatePreference(anyString(), anyString(), anyString(), anyDouble());
    }

    // =====================================================================
    // Test 18: POST /api/profile/preference -- empty strings treated as blank
    // =====================================================================
    @Test
    void testUpdatePreference_BlankFields_ReturnsError() {
        // Arrange
        PreferenceUpdateRequest request = new PreferenceUpdateRequest();
        request.setUserId("   ");
        request.setKey("  ");
        request.setValue("");
        request.setConfidence(0.8);

        // Act
        Result<Void> result = controller.updatePreference(request);

        // Assert
        assertFalse(result.isSuccess(), "Should fail when fields are blank/empty");

        verify(profileService, never()).updatePreference(anyString(), anyString(), anyString(), anyDouble());
    }

    // =====================================================================
    // Test 19: GET /api/profile/{userId}/context -- no profile returns default
    // =====================================================================
    @Test
    void testGetProfileContext_NoProfile_ReturnsDefaultMessage() {
        // Arrange
        when(profileService.getProfileContext("newuser")).thenReturn("No profile data available yet.");

        // Act
        Result<Map<String, Object>> result = controller.getProfileContext("newuser");

        // Assert
        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertEquals("No profile data available yet.", result.getData().get("context"));
    }

    // =====================================================================
    // Test 20: POST /api/profile/preference -- service exception handled
    // =====================================================================
    @Test
    void testUpdatePreference_ServiceException_ReturnsError() {
        // Arrange
        PreferenceUpdateRequest request = new PreferenceUpdateRequest();
        request.setUserId(USER_ID);
        request.setKey("travel_style");
        request.setValue("adventure");
        request.setConfidence(0.9);

        doThrow(new RuntimeException("Redis connection failed"))
                .when(profileService).updatePreference(anyString(), anyString(), anyString(), anyDouble());

        // Act
        Result<Void> result = controller.updatePreference(request);

        // Assert
        assertNotNull(result);
        assertFalse(result.isSuccess(), "Should return error when service throws");
    }
}
