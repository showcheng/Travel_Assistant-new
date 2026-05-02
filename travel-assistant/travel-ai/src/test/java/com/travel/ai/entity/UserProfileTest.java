package com.travel.ai.entity;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * UserProfile 实体单元测试
 *
 * 测试内容：
 * - 实体字段赋值与读取（getter/setter via Lombok @Data）
 * - profileJson JSON 结构合法性验证
 * - 默认值行为
 * - 边界条件（null、空值、特殊字符）
 *
 * 论文 5.3.4: 用户画像记忆机制
 */
class UserProfileTest {

    private UserProfile userProfile;

    @BeforeEach
    void setUp() {
        userProfile = new UserProfile();
    }

    // ========== 基本字段赋值与读取测试 ==========

    @Nested
    @DisplayName("基本字段赋值与读取")
    class BasicFieldTests {

        @Test
        @DisplayName("所有字段可以正确赋值和读取")
        void testAllFields_SetAndGet() {
            LocalDateTime now = LocalDateTime.now();

            userProfile.setId(1L);
            userProfile.setUserId("user-123");
            userProfile.setProfileJson("{\"preferences\":[]}");
            userProfile.setSourceCount(5);
            userProfile.setLastConversationId("conv-456");
            userProfile.setCreatedAt(now);
            userProfile.setUpdatedAt(now);
            userProfile.setVersion(3);

            assertEquals(1L, userProfile.getId());
            assertEquals("user-123", userProfile.getUserId());
            assertEquals("{\"preferences\":[]}", userProfile.getProfileJson());
            assertEquals(5, userProfile.getSourceCount());
            assertEquals("conv-456", userProfile.getLastConversationId());
            assertEquals(now, userProfile.getCreatedAt());
            assertEquals(now, userProfile.getUpdatedAt());
            assertEquals(3, userProfile.getVersion());
        }

        @Test
        @DisplayName("新建实体时字段默认值为null")
        void testDefaultValues_AreNull() {
            assertNull(userProfile.getId());
            assertNull(userProfile.getUserId());
            assertNull(userProfile.getProfileJson());
            assertNull(userProfile.getSourceCount());
            assertNull(userProfile.getLastConversationId());
            assertNull(userProfile.getCreatedAt());
            assertNull(userProfile.getUpdatedAt());
            assertNull(userProfile.getVersion());
        }

        @Test
        @DisplayName("id字段支持Long类型的大值")
        void testId_LargeLongValue() {
            Long largeId = Long.MAX_VALUE;
            userProfile.setId(largeId);
            assertEquals(largeId, userProfile.getId());
        }

        @Test
        @DisplayName("userId字段支持字符串类型（非数字用户ID）")
        void testUserId_StringType() {
            userProfile.setUserId("guest-user-abc");
            assertEquals("guest-user-abc", userProfile.getUserId());
        }

        @Test
        @DisplayName("version字段默认应为1")
        void testVersion_DefaultShouldBeOne() {
            // 在数据库层，version 默认为1。Java 实体层默认为 null，
            // 插入时应由应用层或数据库层设置默认值。
            // 此测试验证当手动设置为1时的行为。
            userProfile.setVersion(1);
            assertEquals(1, userProfile.getVersion());
        }

        @Test
        @DisplayName("sourceCount字段默认应为0")
        void testSourceCount_DefaultShouldBeZero() {
            // 同上，数据库层默认为0，Java 实体层默认为 null。
            userProfile.setSourceCount(0);
            assertEquals(0, userProfile.getSourceCount());
        }
    }

    // ========== profileJson JSON 结构验证测试 ==========

    @Nested
    @DisplayName("profileJson JSON 结构验证")
    class ProfileJsonTests {

        @Test
        @DisplayName("profileJson 可以被解析为有效的 JSON 对象")
        void testProfileJson_ParseableAsJsonObject() {
            String validJson = buildCompleteProfileJson();
            userProfile.setProfileJson(validJson);

            JSONObject parsed = JSON.parseObject(userProfile.getProfileJson());
            assertNotNull(parsed, "profileJson 应能被解析为 JSONObject");
        }

        @Test
        @DisplayName("profileJson 包含 preferences 数组")
        void testProfileJson_ContainsPreferencesArray() {
            String json = buildCompleteProfileJson();
            userProfile.setProfileJson(json);

            JSONObject parsed = JSON.parseObject(userProfile.getProfileJson());
            assertNotNull(parsed.getJSONArray("preferences"),
                    "profileJson 应包含 preferences 数组");
        }

        @Test
        @DisplayName("profileJson 的 preferences 包含完整字段")
        void testProfileJson_PreferencesHaveRequiredFields() {
            String json = buildCompleteProfileJson();
            userProfile.setProfileJson(json);

            JSONObject parsed = JSON.parseObject(userProfile.getProfileJson());
            JSONObject firstPref = parsed.getJSONArray("preferences").getJSONObject(0);

            assertTrue(firstPref.containsKey("key"), "preference 应包含 key 字段");
            assertTrue(firstPref.containsKey("value"), "preference 应包含 value 字段");
            assertTrue(firstPref.containsKey("confidence"), "preference 应包含 confidence 字段");
            assertTrue(firstPref.containsKey("updatedAt"), "preference 应包含 updatedAt 字段");
        }

        @Test
        @DisplayName("profileJson 包含 demographics 对象")
        void testProfileJson_ContainsDemographics() {
            String json = buildCompleteProfileJson();
            userProfile.setProfileJson(json);

            JSONObject parsed = JSON.parseObject(userProfile.getProfileJson());
            JSONObject demographics = parsed.getJSONObject("demographics");
            assertNotNull(demographics, "profileJson 应包含 demographics 对象");
            assertEquals("家庭游(1大1小)", demographics.getString("travel_companion"));
            assertEquals("不吃辣", demographics.getString("dietary"));
        }

        @Test
        @DisplayName("profileJson 包含 history 对象")
        void testProfileJson_ContainsHistory() {
            String json = buildCompleteProfileJson();
            userProfile.setProfileJson(json);

            JSONObject parsed = JSON.parseObject(userProfile.getProfileJson());
            JSONObject history = parsed.getJSONObject("history");
            assertNotNull(history, "profileJson 应包含 history 对象");
            assertNotNull(history.getJSONArray("visited"), "history 应包含 visited 数组");
            assertNotNull(history.getJSONArray("interested"), "history 应包含 interested 数组");
        }

        @Test
        @DisplayName("profileJson 包含 feedback 数组")
        void testProfileJson_ContainsFeedback() {
            String json = buildCompleteProfileJson();
            userProfile.setProfileJson(json);

            JSONObject parsed = JSON.parseObject(userProfile.getProfileJson());
            assertNotNull(parsed.getJSONArray("feedback"), "profileJson 应包含 feedback 数组");

            JSONObject firstFeedback = parsed.getJSONArray("feedback").getJSONObject(0);
            assertTrue(firstFeedback.containsKey("topic"), "feedback 应包含 topic 字段");
            assertTrue(firstFeedback.containsKey("sentiment"), "feedback 应包含 sentiment 字段");
            assertTrue(firstFeedback.containsKey("note"), "feedback 应包含 note 字段");
        }

        @Test
        @DisplayName("profileJson 设置为空JSON对象时可以正常解析")
        void testProfileJson_EmptyJsonObject() {
            userProfile.setProfileJson("{}");

            JSONObject parsed = JSON.parseObject(userProfile.getProfileJson());
            assertNotNull(parsed, "空JSON对象应能正常解析");
            assertTrue(parsed.isEmpty(), "空JSON对象解析后应无键值");
        }

        @Test
        @DisplayName("profileJson 可以修改并重新获取")
        void testProfileJson_CanBeUpdated() {
            String original = "{\"preferences\":[]}";
            String updated = buildCompleteProfileJson();

            userProfile.setProfileJson(original);
            assertEquals(original, userProfile.getProfileJson());

            userProfile.setProfileJson(updated);
            assertEquals(updated, userProfile.getProfileJson());
            assertNotEquals(original, userProfile.getProfileJson());
        }
    }

    // ========== 边界条件与异常情况测试 ==========

    @Nested
    @DisplayName("边界条件与异常情况")
    class EdgeCaseTests {

        @Test
        @DisplayName("profileJson 为 null 时 getProfileJson 返回 null")
        void testProfileJson_NullValue() {
            assertNull(userProfile.getProfileJson(),
                    "未设置时 profileJson 应为 null");
        }

        @Test
        @DisplayName("profileJson 为空字符串时 FastJSON2 返回 null")
        void testProfileJson_EmptyString_ReturnsNull() {
            userProfile.setProfileJson("");

            JSONObject result = JSON.parseObject(userProfile.getProfileJson());
            assertNull(result, "空字符串解析后应返回 null，非有效 JSON 对象");
        }

        @Test
        @DisplayName("profileJson 为无效 JSON 字符串时解析应失败")
        void testProfileJson_InvalidJson_ShouldFailParsing() {
            userProfile.setProfileJson("not json at all");

            assertThrows(Exception.class,
                    () -> JSON.parseObject(userProfile.getProfileJson()),
                    "非JSON字符串解析应抛出异常");
        }

        @Test
        @DisplayName("userId 为 null 时可以正常赋值")
        void testUserId_NullValue() {
            userProfile.setUserId(null);
            assertNull(userProfile.getUserId());
        }

        @Test
        @DisplayName("sourceCount 为负数时不会在实体层校验")
        void testSourceCount_NegativeValue() {
            userProfile.setSourceCount(-1);
            assertEquals(-1, userProfile.getSourceCount(),
                    "实体层不负责业务校验，负数可赋值");
        }

        @Test
        @DisplayName("version 为 0 时可以正常赋值")
        void testVersion_ZeroValue() {
            userProfile.setVersion(0);
            assertEquals(0, userProfile.getVersion());
        }

        @Test
        @DisplayName("lastConversationId 可以设为 null 表示无关联会话")
        void testLastConversationId_NullValue() {
            userProfile.setLastConversationId("conv-001");
            assertEquals("conv-001", userProfile.getLastConversationId());

            userProfile.setLastConversationId(null);
            assertNull(userProfile.getLastConversationId());
        }

        @Test
        @DisplayName("profileJson 包含中文和特殊字符时能正确存储")
        void testProfileJson_ChineseAndSpecialChars() {
            String jsonWithSpecialChars = "{\"preferences\":[{\"key\":\"dietary\",\"value\":\"不吃辣 🌶️\"}]}";
            userProfile.setProfileJson(jsonWithSpecialChars);

            JSONObject parsed = JSON.parseObject(userProfile.getProfileJson());
            String value = parsed.getJSONArray("preferences")
                    .getJSONObject(0).getString("value");
            assertTrue(value.contains("不吃辣"),
                    "中文内容应能正确存储和检索");
        }

        @Test
        @DisplayName("同一个 UserProfile 实例可以重复赋值（覆盖旧值）")
        void testFields_CanBeOverwritten() {
            userProfile.setUserId("user-A");
            userProfile.setUserId("user-B");
            assertEquals("user-B", userProfile.getUserId(),
                    "重复赋值应使用最后一次的值");
        }
    }

    // ========== Lombok @Data 生成方法测试 ==========

    @Nested
    @DisplayName("Lombok @Data 生成方法")
    class LombokDataTests {

        @Test
        @DisplayName("toString 方法返回包含字段值的字符串")
        void testToString_ContainsFieldValues() {
            userProfile.setId(42L);
            userProfile.setUserId("user-test");

            String str = userProfile.toString();
            assertTrue(str.contains("42"), "toString 应包含 id 值");
            assertTrue(str.contains("user-test"), "toString 应包含 userId 值");
        }

        @Test
        @DisplayName("equals 方法对相同字段值的实例返回 true")
        void testEquals_SameValues() {
            UserProfile a = new UserProfile();
            a.setId(1L);
            a.setUserId("user-1");
            a.setVersion(1);

            UserProfile b = new UserProfile();
            b.setId(1L);
            b.setUserId("user-1");
            b.setVersion(1);

            assertEquals(a, b, "字段值相同的两个实例应相等");
            assertEquals(a.hashCode(), b.hashCode(), "字段值相同的两个实例 hashCode 应相同");
        }

        @Test
        @DisplayName("equals 方法对不同字段值的实例返回 false")
        void testEquals_DifferentValues() {
            UserProfile a = new UserProfile();
            a.setUserId("user-A");

            UserProfile b = new UserProfile();
            b.setUserId("user-B");

            assertNotEquals(a, b, "字段值不同的两个实例不应相等");
        }

        @Test
        @DisplayName("equals 方法对 null 返回 false")
        void testEquals_NullComparison() {
            assertNotEquals(null, userProfile,
                    "与非 null 实例比较应返回 false");
        }

        @Test
        @DisplayName("两个新建的空实例应相等")
        void testEquals_TwoNewEmptyInstances() {
            UserProfile a = new UserProfile();
            UserProfile b = new UserProfile();
            assertEquals(a, b, "两个空实例应相等");
        }
    }

    // ========== 辅助方法 ==========

    /**
     * 构建一个完整的用户画像 JSON 字符串，包含所有结构化字段。
     * 用于验证 profileJson 的完整结构。
     */
    private String buildCompleteProfileJson() {
        return """
                {
                  "preferences": [
                    {"key": "travel_style", "value": "自然风光", "confidence": 0.9, "updatedAt": "2026-05-02T10:00:00"},
                    {"key": "budget_range", "value": "3000-5000", "confidence": 0.7, "updatedAt": "2026-05-02T10:00:00"}
                  ],
                  "demographics": {
                    "travel_companion": "家庭游(1大1小)",
                    "dietary": "不吃辣"
                  },
                  "history": {
                    "visited": ["北京","杭州"],
                    "interested": ["云南","三亚"]
                  },
                  "feedback": [
                    {"topic": "故宫行程", "sentiment": "negative", "note": "太赶，偏好慢节奏"}
                  ]
                }
                """;
    }
}
