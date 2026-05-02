package com.travel.ai.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.travel.ai.config.BaseServiceTest;
import com.travel.ai.config.MemoryConfig;
import com.travel.ai.entity.UserProfile;
import com.travel.ai.mapper.UserProfileMapper;
import com.travel.ai.service.UserProfileMemoryService;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * UserProfileMemoryServiceImpl comprehensive unit tests.
 *
 * Tests cover: distillation (LLM + regex fallback), preference CRUD,
 * bounded memory (2KB limit), time decay, profile context formatting,
 * and optimistic locking concurrency handling.
 */
class UserProfileMemoryServiceImplTest extends BaseServiceTest {

    @Mock
    private UserProfileMapper userProfileMapper;

    @Mock
    private ChatLanguageModel chatLanguageModel;

    @Mock
    private MemoryConfig memoryConfig;

    @InjectMocks
    private UserProfileMemoryServiceImpl userProfileMemoryService;

    private static final String TEST_USER_ID = "user-123";
    private static final String TEST_SESSION_ID = "session-456";

    @BeforeEach
    void setUpDefaults() {
        // Provide default config values so tests match the old hardcoded behavior.
        // Using lenient() because not every test uses every config value.
        lenient().when(memoryConfig.getDecayRate()).thenReturn(0.005);
        lenient().when(memoryConfig.getMinConfidence()).thenReturn(0.1);
        lenient().when(memoryConfig.getMaxProfileSize()).thenReturn(2048);
    }

    // ================================================================
    // Helper methods
    // ================================================================

    /**
     * Build a minimal valid profile JSON object with the given preferences.
     */
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

    /**
     * Create a UserProfile entity with the given profileJson string.
     */
    private UserProfile createProfileEntity(String userId, String profileJson) {
        UserProfile profile = new UserProfile();
        profile.setId(1L);
        profile.setUserId(userId);
        profile.setProfileJson(profileJson);
        profile.setSourceCount(1);
        profile.setLastConversationId(TEST_SESSION_ID);
        profile.setCreatedAt(LocalDateTime.now().minusDays(5));
        profile.setUpdatedAt(LocalDateTime.now());
        profile.setVersion(1);
        return profile;
    }

    /**
     * Create a list of chat messages (user/assistant pairs).
     */
    private List<Map<String, String>> createMessages(String... pairs) {
        List<Map<String, String>> messages = new ArrayList<>();
        for (int i = 0; i < pairs.length; i += 2) {
            Map<String, String> userMsg = new HashMap<>();
            userMsg.put("role", "user");
            userMsg.put("content", pairs[i]);
            messages.add(userMsg);

            if (i + 1 < pairs.length) {
                Map<String, String> assistantMsg = new HashMap<>();
                assistantMsg.put("role", "assistant");
                assistantMsg.put("content", pairs[i + 1]);
                messages.add(assistantMsg);
            }
        }
        return messages;
    }

    // ================================================================
    // Distillation tests
    // ================================================================

    @Nested
    @DisplayName("Distillation: extract preferences from conversations")
    class DistillationTests {

        @Test
        @DisplayName("LLM returns valid JSON -> preferences extracted and stored")
        void testDistillFromConversation_LlmSuccess() {
            // Arrange: no existing profile
            when(userProfileMapper.selectOne(any(QueryWrapper.class))).thenReturn(null);

            String llmResponse = """
                {
                  "preferences": [
                    {"key": "travel_style", "value": "自然风光", "confidence": 0.9, "updatedAt": "2026-05-02T10:00:00"}
                  ],
                  "demographics": {"dietary": "不吃辣"},
                  "history": {"interested": ["云南"]},
                  "feedback": []
                }
                """;
            when(chatLanguageModel.generate(anyString())).thenReturn(llmResponse);
            when(userProfileMapper.insert(any(UserProfile.class))).thenReturn(1);

            List<Map<String, String>> messages = createMessages(
                    "我想去云南看自然风光", "云南风景很美，适合您",
                    "我不吃辣", "好的，我会避开辣菜推荐"
            );

            // Act
            userProfileMemoryService.distillFromConversation(TEST_USER_ID, TEST_SESSION_ID, messages);

            // Assert: verify insert was called
            ArgumentCaptor<UserProfile> captor = ArgumentCaptor.forClass(UserProfile.class);
            verify(userProfileMapper).insert(captor.capture());
            UserProfile inserted = captor.getValue();
            assertEquals(TEST_USER_ID, inserted.getUserId());
            assertNotNull(inserted.getProfileJson());

            JSONObject parsed = JSON.parseObject(inserted.getProfileJson());
            JSONArray prefs = parsed.getJSONArray("preferences");
            assertFalse(prefs.isEmpty(), "Should have extracted preferences from LLM response");
        }

        @Test
        @DisplayName("LLM failure -> fallback regex extracts budget from Chinese text")
        void testDistillFromConversation_LlmFails_FallbackRegexBudget() {
            when(userProfileMapper.selectOne(any(QueryWrapper.class))).thenReturn(null);
            when(chatLanguageModel.generate(anyString())).thenThrow(new RuntimeException("API timeout"));
            when(userProfileMapper.insert(any(UserProfile.class))).thenReturn(1);

            List<Map<String, String>> messages = createMessages(
                    "我的预算大概3000到5000元", "好的，我会为您推荐3000-5000元的产品"
            );

            userProfileMemoryService.distillFromConversation(TEST_USER_ID, TEST_SESSION_ID, messages);

            ArgumentCaptor<UserProfile> captor = ArgumentCaptor.forClass(UserProfile.class);
            verify(userProfileMapper).insert(captor.capture());
            String profileJson = captor.getValue().getProfileJson();
            assertNotNull(profileJson);

            // Verify budget was extracted via regex
            assertTrue(profileJson.contains("budget") || profileJson.contains("3000") || profileJson.contains("5000"),
                    "Fallback should extract budget info from Chinese text");
        }

        @Test
        @DisplayName("LLM failure -> fallback regex extracts destination names")
        void testDistillFromConversation_LlmFails_FallbackRegexDestination() {
            when(userProfileMapper.selectOne(any(QueryWrapper.class))).thenReturn(null);
            when(chatLanguageModel.generate(anyString())).thenThrow(new RuntimeException("API error"));
            when(userProfileMapper.insert(any(UserProfile.class))).thenReturn(1);

            List<Map<String, String>> messages = createMessages(
                    "我想去云南和三亚旅游", "云南和三亚都是很好的选择"
            );

            userProfileMemoryService.distillFromConversation(TEST_USER_ID, TEST_SESSION_ID, messages);

            ArgumentCaptor<UserProfile> captor = ArgumentCaptor.forClass(UserProfile.class);
            verify(userProfileMapper).insert(captor.capture());
            String profileJson = captor.getValue().getProfileJson();

            assertTrue(profileJson.contains("云南") || profileJson.contains("三亚"),
                    "Fallback should extract destination names");
        }

        @Test
        @DisplayName("Empty messages list -> no distillation, no errors")
        void testDistillFromConversation_EmptyMessages() {
            userProfileMemoryService.distillFromConversation(TEST_USER_ID, TEST_SESSION_ID, Collections.emptyList());

            // No insert or update should happen
            verify(userProfileMapper, never()).insert(any(UserProfile.class));
            verify(userProfileMapper, never()).updateById(any(UserProfile.class));
        }

        @Test
        @DisplayName("No existing profile -> creates new profile on distillation")
        void testDistillFromConversation_CreatesNewProfile() {
            when(userProfileMapper.selectOne(any(QueryWrapper.class))).thenReturn(null);

            String llmResponse = """
                {
                  "preferences": [],
                  "demographics": {},
                  "history": {"interested": ["北京"]},
                  "feedback": []
                }
                """;
            when(chatLanguageModel.generate(anyString())).thenReturn(llmResponse);
            when(userProfileMapper.insert(any(UserProfile.class))).thenReturn(1);

            List<Map<String, String>> messages = createMessages(
                    "我想去北京", "北京是中国的首都"
            );

            userProfileMemoryService.distillFromConversation(TEST_USER_ID, TEST_SESSION_ID, messages);

            verify(userProfileMapper).insert(any(UserProfile.class));
            verify(userProfileMapper, never()).updateById(any(UserProfile.class));
        }

        @Test
        @DisplayName("Existing profile -> merges new preferences and updates")
        void testDistillFromConversation_MergesWithExisting() {
            JSONObject existingJson = buildProfileJson("travel_style", "自然风光", 0.8,
                    LocalDateTime.now().minusDays(1));
            UserProfile existing = createProfileEntity(TEST_USER_ID, existingJson.toJSONString());
            when(userProfileMapper.selectOne(any(QueryWrapper.class))).thenReturn(existing);

            String llmResponse = """
                {
                  "preferences": [
                    {"key": "budget_range", "value": "5000-8000", "confidence": 0.85, "updatedAt": "2026-05-02T10:00:00"}
                  ],
                  "demographics": {},
                  "history": {},
                  "feedback": []
                }
                """;
            when(chatLanguageModel.generate(anyString())).thenReturn(llmResponse);
            when(userProfileMapper.updateById(any(UserProfile.class))).thenReturn(1);

            List<Map<String, String>> messages = createMessages(
                    "预算大概5000到8000", "好的"
            );

            userProfileMemoryService.distillFromConversation(TEST_USER_ID, TEST_SESSION_ID, messages);

            verify(userProfileMapper).updateById(any(UserProfile.class));
            verify(userProfileMapper, never()).insert(any(UserProfile.class));
        }

        @Test
        @DisplayName("LLM returns invalid JSON -> fallback regex extraction")
        void testDistillFromConversation_LlmReturnsInvalidJson_FallbackRegex() {
            when(userProfileMapper.selectOne(any(QueryWrapper.class))).thenReturn(null);
            when(chatLanguageModel.generate(anyString())).thenReturn("This is not JSON at all");
            when(userProfileMapper.insert(any(UserProfile.class))).thenReturn(1);

            List<Map<String, String>> messages = createMessages(
                    "预算大概2000块左右", "好的"
            );

            userProfileMemoryService.distillFromConversation(TEST_USER_ID, TEST_SESSION_ID, messages);

            ArgumentCaptor<UserProfile> captor = ArgumentCaptor.forClass(UserProfile.class);
            verify(userProfileMapper).insert(captor.capture());
            // Fallback should still create a profile
            assertNotNull(captor.getValue().getProfileJson());
        }
    }

    // ================================================================
    // Preference CRUD tests
    // ================================================================

    @Nested
    @DisplayName("Preference CRUD: get, update, remove preferences")
    class PreferenceCrudTests {

        @Test
        @DisplayName("getProfile returns existing profile")
        void testGetProfile_Existing() {
            JSONObject json = buildProfileJson("travel_style", "自然风光", 0.9, LocalDateTime.now());
            UserProfile entity = createProfileEntity(TEST_USER_ID, json.toJSONString());
            when(userProfileMapper.selectOne(any(QueryWrapper.class))).thenReturn(entity);

            UserProfile result = userProfileMemoryService.getProfile(TEST_USER_ID);

            assertNotNull(result);
            assertEquals(TEST_USER_ID, result.getUserId());
        }

        @Test
        @DisplayName("getProfile returns null for unknown user")
        void testGetProfile_NotFound() {
            when(userProfileMapper.selectOne(any(QueryWrapper.class))).thenReturn(null);

            UserProfile result = userProfileMemoryService.getProfile("unknown-user");

            assertNull(result);
        }

        @Test
        @DisplayName("updatePreference adds new preference to existing profile")
        void testUpdatePreference_AddsNewPreference() {
            JSONObject json = buildProfileJson("travel_style", "自然风光", 0.9, LocalDateTime.now());
            UserProfile existing = createProfileEntity(TEST_USER_ID, json.toJSONString());
            when(userProfileMapper.selectOne(any(QueryWrapper.class))).thenReturn(existing);
            when(userProfileMapper.updateById(any(UserProfile.class))).thenReturn(1);

            userProfileMemoryService.updatePreference(TEST_USER_ID, "dietary", "素食", 0.8);

            ArgumentCaptor<UserProfile> captor = ArgumentCaptor.forClass(UserProfile.class);
            verify(userProfileMapper).updateById(captor.capture());
            JSONObject updated = JSON.parseObject(captor.getValue().getProfileJson());
            JSONArray prefs = updated.getJSONArray("preferences");
            boolean found = false;
            for (int i = 0; i < prefs.size(); i++) {
                if ("dietary".equals(prefs.getJSONObject(i).getString("key"))) {
                    assertEquals("素食", prefs.getJSONObject(i).getString("value"));
                    found = true;
                }
            }
            assertTrue(found, "New preference should be added");
        }

        @Test
        @DisplayName("updatePreference merges with higher confidence")
        void testUpdatePreference_MergesHigherConfidence() {
            JSONObject json = buildProfileJson("travel_style", "自然风光", 0.5, LocalDateTime.now());
            UserProfile existing = createProfileEntity(TEST_USER_ID, json.toJSONString());
            when(userProfileMapper.selectOne(any(QueryWrapper.class))).thenReturn(existing);
            when(userProfileMapper.updateById(any(UserProfile.class))).thenReturn(1);

            // Update with higher confidence
            userProfileMemoryService.updatePreference(TEST_USER_ID, "travel_style", "山水风光", 0.95);

            ArgumentCaptor<UserProfile> captor = ArgumentCaptor.forClass(UserProfile.class);
            verify(userProfileMapper).updateById(captor.capture());
            JSONObject updated = JSON.parseObject(captor.getValue().getProfileJson());
            JSONArray prefs = updated.getJSONArray("preferences");
            JSONObject stylePref = prefs.stream()
                    .map(obj -> (JSONObject) obj)
                    .filter(p -> "travel_style".equals(p.getString("key")))
                    .findFirst().orElse(null);
            assertNotNull(stylePref);
            assertEquals("山水风光", stylePref.getString("value"));
            assertEquals(0.95, stylePref.getDouble("confidence"), 0.001);
        }

        @Test
        @DisplayName("updatePreference creates profile if not exists")
        void testUpdatePreference_CreatesProfileIfNotExists() {
            when(userProfileMapper.selectOne(any(QueryWrapper.class))).thenReturn(null);
            when(userProfileMapper.insert(any(UserProfile.class))).thenReturn(1);

            userProfileMemoryService.updatePreference(TEST_USER_ID, "travel_style", "自然风光", 0.9);

            verify(userProfileMapper).insert(any(UserProfile.class));
        }

        @Test
        @DisplayName("removePreference removes a key successfully")
        void testRemovePreference_RemovesKey() {
            JSONObject json = buildProfileJson("travel_style", "自然风光", 0.9, LocalDateTime.now());
            // Add a second preference
            json.getJSONArray("preferences").add(
                    new JSONObject(Map.of("key", "budget_range", "value", "3000-5000",
                            "confidence", 0.7, "updatedAt", LocalDateTime.now().toString()))
            );
            UserProfile existing = createProfileEntity(TEST_USER_ID, json.toJSONString());
            when(userProfileMapper.selectOne(any(QueryWrapper.class))).thenReturn(existing);
            when(userProfileMapper.updateById(any(UserProfile.class))).thenReturn(1);

            userProfileMemoryService.removePreference(TEST_USER_ID, "travel_style");

            ArgumentCaptor<UserProfile> captor = ArgumentCaptor.forClass(UserProfile.class);
            verify(userProfileMapper).updateById(captor.capture());
            JSONObject updated = JSON.parseObject(captor.getValue().getProfileJson());
            JSONArray prefs = updated.getJSONArray("preferences");
            boolean styleExists = prefs.stream()
                    .anyMatch(p -> "travel_style".equals(((JSONObject) p).getString("key")));
            assertFalse(styleExists, "travel_style should be removed");
            assertEquals(1, prefs.size(), "Only budget_range should remain");
        }

        @Test
        @DisplayName("removePreference on non-existent key does nothing (no error)")
        void testRemovePreference_NonExistentKey_NoError() {
            JSONObject json = buildProfileJson("travel_style", "自然风光", 0.9, LocalDateTime.now());
            UserProfile existing = createProfileEntity(TEST_USER_ID, json.toJSONString());
            when(userProfileMapper.selectOne(any(QueryWrapper.class))).thenReturn(existing);

            // Should not throw, and no update should happen since key not found
            assertDoesNotThrow(() ->
                    userProfileMemoryService.removePreference(TEST_USER_ID, "non_existent_key"));
            verify(userProfileMapper, never()).updateById(any(UserProfile.class));
        }

        @Test
        @DisplayName("getPreferences returns map of key to value")
        void testGetPreferences_ReturnsMap() {
            JSONObject json = buildProfileJson("travel_style", "自然风光", 0.9, LocalDateTime.now());
            json.getJSONArray("preferences").add(
                    new JSONObject(Map.of("key", "budget_range", "value", "3000-5000",
                            "confidence", 0.7, "updatedAt", LocalDateTime.now().toString()))
            );
            UserProfile entity = createProfileEntity(TEST_USER_ID, json.toJSONString());
            when(userProfileMapper.selectOne(any(QueryWrapper.class))).thenReturn(entity);

            Map<String, String> prefs = userProfileMemoryService.getPreferences(TEST_USER_ID);

            assertEquals(2, prefs.size());
            assertEquals("自然风光", prefs.get("travel_style"));
            assertEquals("3000-5000", prefs.get("budget_range"));
        }

        @Test
        @DisplayName("getPreferences returns empty map when no profile exists")
        void testGetPreferences_NoProfile_ReturnsEmptyMap() {
            when(userProfileMapper.selectOne(any(QueryWrapper.class))).thenReturn(null);

            Map<String, String> prefs = userProfileMemoryService.getPreferences("unknown-user");

            assertNotNull(prefs);
            assertTrue(prefs.isEmpty());
        }
    }

    // ================================================================
    // Bounded memory tests
    // ================================================================

    @Nested
    @DisplayName("Bounded memory: enforce 2KB limit on profileJson")
    class BoundedMemoryTests {

        @Test
        @DisplayName("Under 2KB -> no consolidation needed")
        void testEnforceMemoryLimit_Under2KB_NoAction() {
            JSONObject json = buildProfileJson("travel_style", "自然风光", 0.9, LocalDateTime.now());
            UserProfile entity = createProfileEntity(TEST_USER_ID, json.toJSONString());
            when(userProfileMapper.selectOne(any(QueryWrapper.class))).thenReturn(entity);

            userProfileMemoryService.enforceMemoryLimit(TEST_USER_ID);

            // Should not call LLM or update since under limit
            verify(chatLanguageModel, never()).generate(anyString());
            verify(userProfileMapper, never()).updateById(any(UserProfile.class));
        }

        @Test
        @DisplayName("Over 2KB -> LLM consolidates to smaller JSON")
        void testEnforceMemoryLimit_Over2KB_LlmConsolidates() {
            // Build a large profile JSON (over 2048 chars)
            JSONObject json = new JSONObject();
            JSONArray prefs = new JSONArray();
            for (int i = 0; i < 100; i++) {
                JSONObject pref = new JSONObject();
                pref.put("key", "pref_" + i);
                pref.put("value", "value_" + i + "_with_a_long_suffix_to_increase_size");
                pref.put("confidence", 0.5 + (i % 5) * 0.1);
                pref.put("updatedAt", LocalDateTime.now().toString());
                prefs.add(pref);
            }
            json.put("preferences", prefs);
            json.put("demographics", new JSONObject());
            json.put("history", new JSONObject());
            json.put("feedback", new JSONArray());

            assertTrue(json.toJSONString().length() > 2048, "Test data should exceed 2KB");

            UserProfile entity = createProfileEntity(TEST_USER_ID, json.toJSONString());
            when(userProfileMapper.selectOne(any(QueryWrapper.class))).thenReturn(entity);

            // LLM returns a consolidated smaller version
            String consolidated = """
                {
                  "preferences": [
                    {"key": "travel_style", "value": "自然风光", "confidence": 0.9, "updatedAt": "2026-05-02T10:00:00"}
                  ],
                  "demographics": {},
                  "history": {},
                  "feedback": []
                }
                """;
            when(chatLanguageModel.generate(anyString())).thenReturn(consolidated);
            when(userProfileMapper.updateById(any(UserProfile.class))).thenReturn(1);

            userProfileMemoryService.enforceMemoryLimit(TEST_USER_ID);

            verify(chatLanguageModel).generate(anyString());
            verify(userProfileMapper).updateById(any(UserProfile.class));
        }

        @Test
        @DisplayName("Over 2KB and LLM fails -> fallback truncation removes low-confidence items")
        void testEnforceMemoryLimit_Over2KB_LlmFails_FallbackTruncation() {
            // Build a large profile JSON
            JSONObject json = new JSONObject();
            JSONArray prefs = new JSONArray();
            for (int i = 0; i < 100; i++) {
                JSONObject pref = new JSONObject();
                pref.put("key", "pref_" + i);
                pref.put("value", "value_" + i + "_padding_to_increase_size");
                pref.put("confidence", 0.05 + (i % 10) * 0.1);
                pref.put("updatedAt", LocalDateTime.now().toString());
                prefs.add(pref);
            }
            json.put("preferences", prefs);
            json.put("demographics", new JSONObject());
            json.put("history", new JSONObject());
            json.put("feedback", new JSONArray());

            UserProfile entity = createProfileEntity(TEST_USER_ID, json.toJSONString());
            when(userProfileMapper.selectOne(any(QueryWrapper.class))).thenReturn(entity);
            when(chatLanguageModel.generate(anyString())).thenThrow(new RuntimeException("API down"));
            when(userProfileMapper.updateById(any(UserProfile.class))).thenReturn(1);

            userProfileMemoryService.enforceMemoryLimit(TEST_USER_ID);

            // Should still update via fallback truncation
            ArgumentCaptor<UserProfile> captor = ArgumentCaptor.forClass(UserProfile.class);
            verify(userProfileMapper).updateById(captor.capture());
            assertTrue(captor.getValue().getProfileJson().length() <= 2048,
                    "Fallback truncation should bring profile under 2KB");
        }

        @Test
        @DisplayName("No profile exists -> enforceMemoryLimit does nothing")
        void testEnforceMemoryLimit_NoProfile() {
            when(userProfileMapper.selectOne(any(QueryWrapper.class))).thenReturn(null);

            userProfileMemoryService.enforceMemoryLimit("unknown-user");

            verify(chatLanguageModel, never()).generate(anyString());
            verify(userProfileMapper, never()).updateById(any(UserProfile.class));
        }
    }

    // ================================================================
    // Time decay tests
    // ================================================================

    @Nested
    @DisplayName("Time decay: exponential confidence reduction for old preferences")
    class TimeDecayTests {

        @Test
        @DisplayName("Old preferences have reduced confidence on load")
        void testTimeDecay_OldPreference_ReducedConfidence() {
            // Preference updated 100 days ago with confidence 0.9
            LocalDateTime oldDate = LocalDateTime.now().minusDays(100);
            JSONObject json = buildProfileJson("travel_style", "自然风光", 0.9, oldDate);
            UserProfile entity = createProfileEntity(TEST_USER_ID, json.toJSONString());
            when(userProfileMapper.selectOne(any(QueryWrapper.class))).thenReturn(entity);

            // getProfile applies time decay internally and persists
            UserProfile result = userProfileMemoryService.getProfile(TEST_USER_ID);

            assertNotNull(result);
            JSONObject parsed = JSON.parseObject(result.getProfileJson());
            JSONArray prefs = parsed.getJSONArray("preferences");
            double decayedConfidence = prefs.getJSONObject(0).getDouble("confidence");
            // After 100 days: 0.9 * exp(-0.005 * 100) = 0.9 * exp(-0.5) ~ 0.9 * 0.606 ~ 0.545
            assertTrue(decayedConfidence < 0.9,
                    "Old preference should have lower confidence: actual=" + decayedConfidence);
            assertTrue(decayedConfidence > 0.1,
                    "Should still be above removal threshold: actual=" + decayedConfidence);
        }

        @Test
        @DisplayName("Preferences below 0.1 threshold are removed on load")
        void testTimeDecay_BelowThreshold_Removed() {
            // Preference updated 600 days ago with confidence 0.5
            // decayed = 0.5 * exp(-0.005 * 600) = 0.5 * exp(-3) = 0.5 * 0.0498 ~ 0.025
            LocalDateTime veryOldDate = LocalDateTime.now().minusDays(600);
            JSONObject json = buildProfileJson("old_pref", "old_value", 0.5, veryOldDate);
            UserProfile entity = createProfileEntity(TEST_USER_ID, json.toJSONString());
            when(userProfileMapper.selectOne(any(QueryWrapper.class))).thenReturn(entity);

            UserProfile result = userProfileMemoryService.getProfile(TEST_USER_ID);

            assertNotNull(result);
            JSONObject parsed = JSON.parseObject(result.getProfileJson());
            JSONArray prefs = parsed.getJSONArray("preferences");
            // The preference should be removed because decayed confidence < 0.1
            assertTrue(prefs.isEmpty(),
                    "Preference below 0.1 threshold should be removed: prefs=" + prefs);
        }

        @Test
        @DisplayName("Recent preferences are not significantly decayed")
        void testTimeDecay_RecentPreference_MinimalDecay() {
            // Preference updated 1 day ago
            LocalDateTime recentDate = LocalDateTime.now().minusDays(1);
            JSONObject json = buildProfileJson("travel_style", "自然风光", 0.9, recentDate);
            UserProfile entity = createProfileEntity(TEST_USER_ID, json.toJSONString());
            when(userProfileMapper.selectOne(any(QueryWrapper.class))).thenReturn(entity);

            UserProfile result = userProfileMemoryService.getProfile(TEST_USER_ID);

            assertNotNull(result);
            JSONObject parsed = JSON.parseObject(result.getProfileJson());
            double confidence = parsed.getJSONArray("preferences").getJSONObject(0).getDouble("confidence");
            // After 1 day: 0.9 * exp(-0.005 * 1) ~ 0.9 * 0.995 ~ 0.896
            assertTrue(confidence >= 0.89,
                    "Recent preference should barely decay: actual=" + confidence);
        }

        @Test
        @DisplayName("Time decay is applied when profile is loaded via getProfile")
        void testTimeDecay_AppliedOnGetProfile() {
            LocalDateTime oldDate = LocalDateTime.now().minusDays(50);
            JSONObject json = buildProfileJson("budget_range", "3000-5000", 0.7, oldDate);
            UserProfile entity = createProfileEntity(TEST_USER_ID, json.toJSONString());
            when(userProfileMapper.selectOne(any(QueryWrapper.class))).thenReturn(entity);

            UserProfile result = userProfileMemoryService.getProfile(TEST_USER_ID);

            assertNotNull(result);
            // Verify the profile JSON contains decayed confidence
            JSONObject parsed = JSON.parseObject(result.getProfileJson());
            double confidence = parsed.getJSONArray("preferences").getJSONObject(0).getDouble("confidence");
            assertTrue(confidence < 0.7,
                    "Loaded profile should show decayed confidence: actual=" + confidence);
        }
    }

    // ================================================================
    // Profile context formatting tests
    // ================================================================

    @Nested
    @DisplayName("Profile context: formatted text for LLM injection")
    class ProfileContextTests {

        @Test
        @DisplayName("getProfileContext formats profile as readable text")
        void testGetProfileContext_FormatsProfile() {
            JSONObject json = buildProfileJson("travel_style", "自然风光", 0.9, LocalDateTime.now());
            json.getJSONArray("preferences").add(
                    new JSONObject(Map.of("key", "budget_range", "value", "3000-5000",
                            "confidence", 0.7, "updatedAt", LocalDateTime.now().toString()))
            );
            UserProfile entity = createProfileEntity(TEST_USER_ID, json.toJSONString());
            when(userProfileMapper.selectOne(any(QueryWrapper.class))).thenReturn(entity);

            String context = userProfileMemoryService.getProfileContext(TEST_USER_ID);

            assertNotNull(context);
            assertFalse(context.isEmpty());
            assertTrue(context.contains("travel_style") || context.contains("自然风光"),
                    "Context should contain preference info");
        }

        @Test
        @DisplayName("getProfileContext returns default message when no profile")
        void testGetProfileContext_NoProfile_DefaultMessage() {
            when(userProfileMapper.selectOne(any(QueryWrapper.class))).thenReturn(null);

            String context = userProfileMemoryService.getProfileContext("unknown-user");

            assertNotNull(context);
            // Should return something indicating no profile (empty string or default message)
            // The contract says "default message", so it should not be null
        }
    }

    // ================================================================
    // Concurrent update (optimistic lock) tests
    // ================================================================

    @Nested
    @DisplayName("Concurrency: optimistic lock failure handling")
    class ConcurrencyTests {

        @Test
        @DisplayName("Optimistic lock failure on updateById -> handled gracefully with retry")
        void testOptimisticLockFailure_HandledGracefully() {
            JSONObject json = buildProfileJson("travel_style", "自然风光", 0.9, LocalDateTime.now());
            UserProfile existing = createProfileEntity(TEST_USER_ID, json.toJSONString());
            when(userProfileMapper.selectOne(any(QueryWrapper.class))).thenReturn(existing);
            // First update fails (lock), second succeeds
            when(userProfileMapper.updateById(any(UserProfile.class)))
                    .thenReturn(0)  // 0 rows affected = lock failure
                    .thenReturn(1); // retry succeeds

            // The service should handle this gracefully (retry or log)
            assertDoesNotThrow(() ->
                    userProfileMemoryService.updatePreference(TEST_USER_ID, "new_key", "new_value", 0.8));
        }

        @Test
        @DisplayName("updatePreference with null key does not cause errors")
        void testUpdatePreference_NullKey_Handled() {
            // Null key should be handled gracefully with early return
            assertDoesNotThrow(() ->
                    userProfileMemoryService.updatePreference(TEST_USER_ID, null, "value", 0.5));
            verify(userProfileMapper, never()).selectOne(any(QueryWrapper.class));
        }
    }

    // ================================================================
    // Edge case tests
    // ================================================================

    @Nested
    @DisplayName("Edge cases: null, empty, boundary values")
    class EdgeCaseTests {

        @Test
        @DisplayName("distillFromConversation with null messages -> no error")
        void testDistill_NullMessages() {
            assertDoesNotThrow(() ->
                    userProfileMemoryService.distillFromConversation(TEST_USER_ID, TEST_SESSION_ID, null));
        }

        @Test
        @DisplayName("getProfileContext with null userId -> returns default")
        void testGetProfileContext_NullUserId() {
            when(userProfileMapper.selectOne(any(QueryWrapper.class))).thenReturn(null);

            String context = userProfileMemoryService.getProfileContext(null);
            assertNotNull(context);
        }

        @Test
        @DisplayName("updatePreference with confidence at boundaries (0.0 and 1.0)")
        void testUpdatePreference_ConfidenceBoundaries() {
            JSONObject json = buildProfileJson("existing", "val", 0.5, LocalDateTime.now());
            UserProfile existing = createProfileEntity(TEST_USER_ID, json.toJSONString());
            when(userProfileMapper.selectOne(any(QueryWrapper.class))).thenReturn(existing);
            when(userProfileMapper.updateById(any(UserProfile.class))).thenReturn(1);

            // Boundary: 0.0
            assertDoesNotThrow(() ->
                    userProfileMemoryService.updatePreference(TEST_USER_ID, "zero_conf", "value", 0.0));

            // Boundary: 1.0
            assertDoesNotThrow(() ->
                    userProfileMemoryService.updatePreference(TEST_USER_ID, "max_conf", "value", 1.0));
        }

        @Test
        @DisplayName("distillFromConversation merges history interested destinations")
        void testDistill_MergesHistoryInterested() {
            JSONObject existingJson = new JSONObject();
            existingJson.put("preferences", new JSONArray());
            existingJson.put("demographics", new JSONObject());
            JSONObject history = new JSONObject();
            history.put("visited", new JSONArray(List.of("北京")));
            history.put("interested", new JSONArray(List.of("云南")));
            existingJson.put("history", history);
            existingJson.put("feedback", new JSONArray());

            UserProfile existing = createProfileEntity(TEST_USER_ID, existingJson.toJSONString());
            when(userProfileMapper.selectOne(any(QueryWrapper.class))).thenReturn(existing);

            String llmResponse = """
                {
                  "preferences": [],
                  "demographics": {},
                  "history": {"interested": ["三亚", "云南"]},
                  "feedback": []
                }
                """;
            when(chatLanguageModel.generate(anyString())).thenReturn(llmResponse);
            when(userProfileMapper.updateById(any(UserProfile.class))).thenReturn(1);

            List<Map<String, String>> messages = createMessages(
                    "我想去三亚", "三亚风景很美"
            );

            userProfileMemoryService.distillFromConversation(TEST_USER_ID, TEST_SESSION_ID, messages);

            ArgumentCaptor<UserProfile> captor = ArgumentCaptor.forClass(UserProfile.class);
            verify(userProfileMapper).updateById(captor.capture());
            JSONObject updated = JSON.parseObject(captor.getValue().getProfileJson());
            JSONArray interested = updated.getJSONObject("history").getJSONArray("interested");

            // Should contain both old (云南) and new (三亚), deduplicated
            assertTrue(interested.contains("云南"), "Should retain existing interested destination");
            assertTrue(interested.contains("三亚"), "Should add new interested destination");
        }
    }
}
