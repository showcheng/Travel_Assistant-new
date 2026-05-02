package com.travel.ai.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.travel.ai.config.BaseServiceTest;
import com.travel.ai.config.MemoryConfig;
import com.travel.ai.entity.UserProfile;
import com.travel.ai.mapper.UserProfileMapper;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * MemoryCleanupTest: tests for the scheduled cleanup job and
 * configurable decay constants in UserProfileMemoryServiceImpl.
 *
 * Covers: cleanup of expired preferences, deletion of empty profiles,
 * healthy profiles left untouched, empty DB, and scheduled method invocability.
 */
class MemoryCleanupTest extends BaseServiceTest {

    @Mock
    private UserProfileMapper userProfileMapper;

    @Mock
    private ChatLanguageModel chatLanguageModel;

    @Mock
    private MemoryConfig memoryConfig;

    @InjectMocks
    private UserProfileMemoryServiceImpl service;

    private static final String USER_ID = "cleanup-user-001";

    @BeforeEach
    void setUpConfig() {
        // Provide default config values for all tests using lenient stubs
        // since not every test uses every config getter
        lenient().when(memoryConfig.getDecayRate()).thenReturn(0.005);
        lenient().when(memoryConfig.getMinConfidence()).thenReturn(0.1);
        lenient().when(memoryConfig.getMaxProfileSize()).thenReturn(2048);
        lenient().when(memoryConfig.getCleanupIntervalMinutes()).thenReturn(60);
        lenient().when(memoryConfig.getDistillationMinMessages()).thenReturn(4);
    }

    // ================================================================
    // Helper methods
    // ================================================================

    private JSONObject buildProfileJson(String key, String value, double confidence, LocalDateTime updatedAt) {
        JSONObject profile = new JSONObject();
        JSONArray prefs = new JSONArray();
        JSONObject pref = new JSONObject();
        pref.put("key", key);
        pref.put("value", value);
        pref.put("confidence", confidence);
        pref.put("updatedAt", updatedAt.toString());
        prefs.add(pref);
        profile.put("preferences", prefs);
        profile.put("demographics", new JSONObject());
        profile.put("history", new JSONObject());
        profile.put("feedback", new JSONArray());
        return profile;
    }

    private UserProfile createProfileEntity(String userId, String profileJson) {
        UserProfile profile = new UserProfile();
        profile.setId(1L);
        profile.setUserId(userId);
        profile.setProfileJson(profileJson);
        profile.setSourceCount(1);
        profile.setCreatedAt(LocalDateTime.now().minusDays(5));
        profile.setUpdatedAt(LocalDateTime.now());
        profile.setVersion(1);
        return profile;
    }

    // ================================================================
    // Scheduled cleanup tests
    // ================================================================

    @Nested
    @DisplayName("Scheduled cleanup: cleanupExpiredPreferences")
    class ScheduledCleanupTests {

        @Test
        @DisplayName("cleanup removes low-confidence expired preferences from all profiles")
        void testCleanup_RemovesExpiredPreferences() {
            // Preference 600 days old with confidence 0.5 => decays to ~0.025, below 0.1
            LocalDateTime veryOld = LocalDateTime.now().minusDays(600);
            JSONObject json = buildProfileJson("old_pref", "old_value", 0.5, veryOld);
            // Also add a recent preference that should survive
            JSONObject recentPref = new JSONObject();
            recentPref.put("key", "recent_pref");
            recentPref.put("value", "recent_value");
            recentPref.put("confidence", 0.9);
            recentPref.put("updatedAt", LocalDateTime.now().minusDays(1).toString());
            json.getJSONArray("preferences").add(recentPref);

            UserProfile profile = createProfileEntity(USER_ID, json.toJSONString());
            List<UserProfile> allProfiles = List.of(profile);
            when(userProfileMapper.selectList(any(QueryWrapper.class))).thenReturn(allProfiles);
            when(userProfileMapper.updateById(any(UserProfile.class))).thenReturn(1);

            service.cleanupExpiredPreferences();

            ArgumentCaptor<UserProfile> captor = ArgumentCaptor.forClass(UserProfile.class);
            verify(userProfileMapper).updateById(captor.capture());
            JSONObject updated = JSON.parseObject(captor.getValue().getProfileJson());
            JSONArray prefs = updated.getJSONArray("preferences");

            // old_pref should be removed, recent_pref should remain
            boolean oldRemoved = prefs.stream()
                    .noneMatch(p -> "old_pref".equals(((JSONObject) p).getString("key")));
            boolean recentRemains = prefs.stream()
                    .anyMatch(p -> "recent_pref".equals(((JSONObject) p).getString("key")));

            assertTrue(oldRemoved, "Old expired preference should be removed");
            assertTrue(recentRemains, "Recent healthy preference should remain");
        }

        @Test
        @DisplayName("cleanup deletes profiles that become empty after preference removal")
        void testCleanup_DeletesEmptyProfiles() {
            // Profile with only one very old preference => will become empty after cleanup
            LocalDateTime veryOld = LocalDateTime.now().minusDays(600);
            JSONObject json = buildProfileJson("only_pref", "only_value", 0.3, veryOld);
            // Ensure demographics/history/feedback are empty so the profile is truly empty
            json.put("demographics", new JSONObject());
            json.put("history", new JSONObject());
            json.put("feedback", new JSONArray());

            UserProfile profile = createProfileEntity(USER_ID, json.toJSONString());
            List<UserProfile> allProfiles = List.of(profile);
            when(userProfileMapper.selectList(any(QueryWrapper.class))).thenReturn(allProfiles);
            when(userProfileMapper.deleteById(anyLong())).thenReturn(1);

            service.cleanupExpiredPreferences();

            // Should delete the profile since it became empty
            verify(userProfileMapper).deleteById(profile.getId());
            // Should NOT call updateById since profile is deleted instead
            verify(userProfileMapper, never()).updateById(any(UserProfile.class));
        }

        @Test
        @DisplayName("cleanup skips healthy profiles that need no changes")
        void testCleanup_SkipsHealthyProfiles() {
            // Use LocalDateTime.now() so 0 days pass => confidence unchanged => JSON identical
            LocalDateTime now = LocalDateTime.now();
            JSONObject json = buildProfileJson("good_pref", "good_value", 0.95, now);

            UserProfile profile = createProfileEntity(USER_ID, json.toJSONString());
            List<UserProfile> allProfiles = List.of(profile);
            when(userProfileMapper.selectList(any(QueryWrapper.class))).thenReturn(allProfiles);

            service.cleanupExpiredPreferences();

            // No update or delete should occur since JSON is identical after decay
            verify(userProfileMapper, never()).updateById(any(UserProfile.class));
            verify(userProfileMapper, never()).deleteById(anyLong());
        }

        @Test
        @DisplayName("cleanup with no profiles in DB completes without errors")
        void testCleanup_NoProfiles() {
            when(userProfileMapper.selectList(any(QueryWrapper.class))).thenReturn(Collections.emptyList());

            assertDoesNotThrow(() -> service.cleanupExpiredPreferences());

            verify(userProfileMapper, never()).updateById(any(UserProfile.class));
            verify(userProfileMapper, never()).deleteById(anyLong());
        }

        @Test
        @DisplayName("scheduled cleanup method is callable and does not throw")
        void testCleanup_CallableWithoutError() {
            when(userProfileMapper.selectList(any(QueryWrapper.class))).thenReturn(Collections.emptyList());

            assertDoesNotThrow(() -> service.cleanupExpiredPreferences(),
                    "cleanupExpiredPreferences should be safely callable");
        }

        @Test
        @DisplayName("cleanup handles multiple profiles in a single pass")
        void testCleanup_MultipleProfiles() {
            // Profile 1: has one expired pref
            LocalDateTime veryOld = LocalDateTime.now().minusDays(600);
            JSONObject json1 = buildProfileJson("expired", "value", 0.3, veryOld);
            json1.put("demographics", new JSONObject());
            json1.put("history", new JSONObject());
            json1.put("feedback", new JSONArray());
            UserProfile profile1 = createProfileEntity("user-1", json1.toJSONString());
            profile1.setId(1L);

            // Profile 2: healthy - use LocalDateTime.now() so 0 days of decay
            LocalDateTime now = LocalDateTime.now();
            JSONObject json2 = buildProfileJson("healthy", "value", 0.9, now);
            UserProfile profile2 = createProfileEntity("user-2", json2.toJSONString());
            profile2.setId(2L);

            List<UserProfile> allProfiles = List.of(profile1, profile2);
            when(userProfileMapper.selectList(any(QueryWrapper.class))).thenReturn(allProfiles);
            when(userProfileMapper.deleteById(1L)).thenReturn(1);

            service.cleanupExpiredPreferences();

            // Profile 1 should be deleted (became empty), profile 2 untouched
            verify(userProfileMapper).deleteById(1L);
            verify(userProfileMapper, never()).updateById(any(UserProfile.class));
        }
    }

    // ================================================================
    // Configurable decay constants tests
    // ================================================================

    @Nested
    @DisplayName("Configurable decay constants via MemoryConfig")
    class ConfigurableDecayTests {

        @Test
        @DisplayName("Custom decay rate from MemoryConfig is used instead of hardcoded value")
        void testCustomDecayRate() {
            // Use aggressive decay rate: 0.05 instead of 0.005
            when(memoryConfig.getDecayRate()).thenReturn(0.05);

            // Preference 50 days old with confidence 0.8
            // With decay 0.05: 0.8 * exp(-0.05*50) = 0.8 * exp(-2.5) = 0.8 * 0.082 ~ 0.066
            // That is below default threshold 0.1, so it should be removed
            LocalDateTime oldDate = LocalDateTime.now().minusDays(50);
            JSONObject json = buildProfileJson("aggressive_decay", "value", 0.8, oldDate);
            UserProfile entity = createProfileEntity(USER_ID, json.toJSONString());
            when(userProfileMapper.selectOne(any(QueryWrapper.class))).thenReturn(entity);

            UserProfile result = service.getProfile(USER_ID);

            assertNotNull(result);
            JSONObject parsed = JSON.parseObject(result.getProfileJson());
            JSONArray prefs = parsed.getJSONArray("preferences");
            assertTrue(prefs.isEmpty(),
                    "With aggressive decay rate, preference should fall below threshold and be removed");
        }

        @Test
        @DisplayName("Custom minConfidence threshold from MemoryConfig is applied")
        void testCustomMinConfidence() {
            // Use higher threshold: 0.5 instead of default 0.1
            when(memoryConfig.getMinConfidence()).thenReturn(0.5);

            // Preference 100 days old with confidence 0.7
            // decayed: 0.7 * exp(-0.005*100) = 0.7 * 0.606 = 0.424
            // With threshold 0.5, this should be removed (0.424 < 0.5)
            LocalDateTime oldDate = LocalDateTime.now().minusDays(100);
            JSONObject json = buildProfileJson("high_threshold", "value", 0.7, oldDate);
            UserProfile entity = createProfileEntity(USER_ID, json.toJSONString());
            when(userProfileMapper.selectOne(any(QueryWrapper.class))).thenReturn(entity);

            UserProfile result = service.getProfile(USER_ID);

            assertNotNull(result);
            JSONObject parsed = JSON.parseObject(result.getProfileJson());
            JSONArray prefs = parsed.getJSONArray("preferences");
            assertTrue(prefs.isEmpty(),
                    "With higher threshold, preference should be removed");
        }

        @Test
        @DisplayName("Custom maxProfileSize from MemoryConfig is used for enforceMemoryLimit")
        void testCustomMaxProfileSize() {
            // Use smaller size: 300 instead of 2048
            when(memoryConfig.getMaxProfileSize()).thenReturn(300);

            // Build profile that is over 300 chars but under 2048
            JSONObject json = new JSONObject();
            JSONArray prefs = new JSONArray();
            for (int i = 0; i < 5; i++) {
                JSONObject pref = new JSONObject();
                pref.put("key", "pref_" + i);
                pref.put("value", "val_" + i);
                pref.put("confidence", 0.5);
                pref.put("updatedAt", LocalDateTime.now().toString());
                prefs.add(pref);
            }
            json.put("preferences", prefs);
            json.put("demographics", new JSONObject());
            json.put("history", new JSONObject());
            json.put("feedback", new JSONArray());

            int originalSize = json.toJSONString().length();
            // With short keys/values, 5 prefs should be under 300 chars, so add more padding
            // Let's use 10 prefs to be safe
            prefs.clear();
            for (int i = 0; i < 15; i++) {
                JSONObject pref = new JSONObject();
                pref.put("key", "preference_key_" + i);
                pref.put("value", "some_value_" + i);
                pref.put("confidence", 0.5);
                pref.put("updatedAt", LocalDateTime.now().toString());
                prefs.add(pref);
            }

            originalSize = json.toJSONString().length();
            assertTrue(originalSize > 300, "Test data should exceed custom 300 char limit, was: " + originalSize);
            assertTrue(originalSize < 2048, "Test data should be under default 2048 limit, was: " + originalSize);

            UserProfile entity = createProfileEntity(USER_ID, json.toJSONString());
            when(userProfileMapper.selectOne(any(QueryWrapper.class))).thenReturn(entity);
            when(chatLanguageModel.generate(anyString())).thenThrow(new RuntimeException("LLM unavailable"));
            when(userProfileMapper.updateById(any(UserProfile.class))).thenReturn(1);

            service.enforceMemoryLimit(USER_ID);

            verify(userProfileMapper).updateById(any(UserProfile.class));
        }
    }

    // ================================================================
    // Edge cases for cleanup
    // ================================================================

    @Nested
    @DisplayName("Cleanup edge cases")
    class CleanupEdgeCaseTests {

        @Test
        @DisplayName("cleanup with null profileJson in a profile entity is handled safely")
        void testCleanup_NullProfileJson() {
            UserProfile profile = createProfileEntity(USER_ID, null);
            List<UserProfile> allProfiles = List.of(profile);
            when(userProfileMapper.selectList(any(QueryWrapper.class))).thenReturn(allProfiles);

            assertDoesNotThrow(() -> service.cleanupExpiredPreferences());
        }

        @Test
        @DisplayName("cleanup with empty profileJson is handled safely")
        void testCleanup_EmptyProfileJson() {
            UserProfile profile = createProfileEntity(USER_ID, "");
            List<UserProfile> allProfiles = List.of(profile);
            when(userProfileMapper.selectList(any(QueryWrapper.class))).thenReturn(allProfiles);

            assertDoesNotThrow(() -> service.cleanupExpiredPreferences());
            verify(userProfileMapper, never()).updateById(any(UserProfile.class));
            verify(userProfileMapper, never()).deleteById(anyLong());
        }

        @Test
        @DisplayName("cleanup with invalid JSON profileJson is handled safely")
        void testCleanup_InvalidJson() {
            UserProfile profile = createProfileEntity(USER_ID, "this is not json");
            List<UserProfile> allProfiles = List.of(profile);
            when(userProfileMapper.selectList(any(QueryWrapper.class))).thenReturn(allProfiles);

            assertDoesNotThrow(() -> service.cleanupExpiredPreferences());
        }

        @Test
        @DisplayName("profile with non-preference data (demographics, history) is not deleted even if preferences expire")
        void testCleanup_ProfileWithDataNotDeleted() {
            // All preferences expired, but demographics has data => profile should NOT be deleted
            LocalDateTime veryOld = LocalDateTime.now().minusDays(600);
            JSONObject json = buildProfileJson("expired", "value", 0.3, veryOld);
            JSONObject demographics = new JSONObject();
            demographics.put("dietary", "素食");
            json.put("demographics", demographics);

            UserProfile profile = createProfileEntity(USER_ID, json.toJSONString());
            List<UserProfile> allProfiles = List.of(profile);
            when(userProfileMapper.selectList(any(QueryWrapper.class))).thenReturn(allProfiles);
            when(userProfileMapper.updateById(any(UserProfile.class))).thenReturn(1);

            service.cleanupExpiredPreferences();

            // Should update (removing expired pref) but NOT delete (demographics still has data)
            verify(userProfileMapper).updateById(any(UserProfile.class));
            verify(userProfileMapper, never()).deleteById(anyLong());
        }

        @Test
        @DisplayName("cleanup is resilient to DB errors during selectList")
        void testCleanup_DbSelectError() {
            when(userProfileMapper.selectList(any(QueryWrapper.class)))
                    .thenThrow(new RuntimeException("DB connection lost"));

            assertDoesNotThrow(() -> service.cleanupExpiredPreferences(),
                    "Cleanup should handle DB errors gracefully");
        }

        @Test
        @DisplayName("cleanup is resilient to DB errors during updateById for individual profiles")
        void testCleanup_DbUpdateError() {
            LocalDateTime veryOld = LocalDateTime.now().minusDays(600);
            JSONObject json = buildProfileJson("expired", "value", 0.3, veryOld);
            JSONObject demographics = new JSONObject();
            demographics.put("dietary", "素食");
            json.put("demographics", demographics);

            UserProfile profile = createProfileEntity(USER_ID, json.toJSONString());
            List<UserProfile> allProfiles = List.of(profile);
            when(userProfileMapper.selectList(any(QueryWrapper.class))).thenReturn(allProfiles);
            when(userProfileMapper.updateById(any(UserProfile.class)))
                    .thenThrow(new RuntimeException("Update failed"));

            // Should not throw even if individual profile update fails
            assertDoesNotThrow(() -> service.cleanupExpiredPreferences());
        }
    }
}
