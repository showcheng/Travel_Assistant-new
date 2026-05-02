package com.travel.ai.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.travel.ai.config.MemoryConfig;
import com.travel.ai.entity.UserProfile;
import com.travel.ai.mapper.UserProfileMapper;
import com.travel.ai.service.UserProfileMemoryService;
import dev.langchain4j.model.chat.ChatLanguageModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 用户画像记忆服务实现
 *
 * 从对话中蒸馏提取用户旅行偏好，维护有界结构化画像，
 * 并对较旧的偏好应用时间衰减。使用 LLM 进行智能提取，
 * LLM 不可用时降级到基于正则的启发式方法。
 *
 * 论文 5.3.4: 用户画像记忆机制
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserProfileMemoryServiceImpl implements UserProfileMemoryService {

    private final UserProfileMapper userProfileMapper;
    private final ChatLanguageModel chatLanguageModel;
    private final MemoryConfig memoryConfig;

    /** Maximum profileJson length in characters (2KB) - kept for backward compatibility */
    static final int MAX_PROFILE_JSON_LENGTH = 2048;

    /** Maximum retry count for optimistic lock failures */
    private static final int MAX_OPTIMISTIC_LOCK_RETRIES = 3;

    /** Known Chinese destination names for regex fallback extraction */
    private static final Set<String> KNOWN_DESTINATIONS = Set.of(
            "北京", "上海", "广州", "深圳", "杭州", "成都", "重庆", "西安",
            "南京", "苏州", "厦门", "青岛", "大连", "三亚", "昆明", "大理",
            "丽江", "桂林", "张家界", "九寨沟", "黄山", "泰山", "武夷山",
            "乌镇", "周庄", "凤凰古城", "敦煌", "拉萨", "林芝", "云南",
            "四川", "湖南", "贵州", "海南", "西藏", "新疆"
    );

    /** Regex pattern to match budget amounts in Chinese text */
    private static final Pattern BUDGET_PATTERN = Pattern.compile(
            "(\\d{1,5})\\s*(元|块钱?|块|RMB|rmb)[^\\d]*?(?:到|至|-|~)?\\s*(\\d{1,5})?\\s*(元|块钱?|块|RMB|rmb)?"
    );

    /** Regex pattern to match dietary preferences */
    private static final Pattern DIETARY_PATTERN = Pattern.compile(
            "(不吃辣|吃辣|素食|不吃肉|清真|忌口|过敏|海鲜过敏|花生过敏)"
    );

    /** Regex pattern to match companion information */
    private static final Pattern COMPANION_PATTERN = Pattern.compile(
            "(家庭游|带(?:小孩|孩子|宝宝)|情侣|蜜月|独自|一个人|跟团|自驾|父母|老人|亲子)"
    );

    /** LLM prompt for distilling preferences from conversation */
    private static final String DISTILLATION_PROMPT_TEMPLATE =
            "你是一个旅行偏好分析专家。请从以下对话中提取用户的旅行偏好信息。\n\n"
            + "请返回严格的JSON格式，包含以下字段：\n"
            + "{\n"
            + "  \"preferences\": [\n"
            + "    {\"key\": \"偏好键\", \"value\": \"偏好值\", \"confidence\": 0.0-1.0, \"updatedAt\": \"ISO时间\"}\n"
            + "  ],\n"
            + "  \"demographics\": {\"travel_companion\": \"同行人\", \"dietary\": \"饮食偏好\"},\n"
            + "  \"history\": {\"visited\": [\"去过的地方\"], \"interested\": [\"感兴趣的地方\"]},\n"
            + "  \"feedback\": [{\"topic\": \"话题\", \"sentiment\": \"positive/negative/neutral\", \"note\": \"备注\"}]\n"
            + "}\n\n"
            + "偏好键可选：travel_style, budget_range, travel_companion, dietary, pace_preference\n"
            + "confidence越高表示越确定。只提取明确提到的信息，不要猜测。\n"
            + "只返回JSON，不要返回其他内容。\n\n"
            + "对话内容：\n%s";

    /** LLM prompt for consolidating an oversized profile */
    private static final String CONSOLIDATION_PROMPT_TEMPLATE =
            "用户的画像数据过大，需要精简。请合并相似偏好，移除低价值信息，保留核心特征。\n"
            + "精简后必须保留JSON结构，且不超过1500字符。\n\n"
            + "原始数据：\n%s\n\n"
            + "精简后的JSON：";

    // ================================================================
    // Public API
    // ================================================================

    @Override
    public UserProfile getProfile(String userId) {
        UserProfile profile = findProfileByUserId(userId);
        if (profile == null) {
            return null;
        }
        String profileJson = applyTimeDecay(profile.getProfileJson());
        profile.setProfileJson(profileJson);
        return profile;
    }

    @Override
    public String getProfileContext(String userId) {
        UserProfile profile = getProfile(userId);
        if (profile == null || profile.getProfileJson() == null || profile.getProfileJson().isBlank()) {
            return "暂无用户画像信息。";
        }
        return formatProfileAsContext(profile.getProfileJson());
    }

    @Override
    public void distillFromConversation(String userId, String sessionId, List<Map<String, String>> messages) {
        if (messages == null || messages.isEmpty()) {
            log.debug("Empty messages, skip distillation: userId={}", userId);
            return;
        }

        JSONObject extracted;
        try {
            extracted = distillWithLlm(messages);
        } catch (Exception e) {
            log.warn("LLM distillation failed, falling back to regex: {}", e.getMessage());
            extracted = distillWithRegex(messages);
        }

        if (extracted == null) {
            extracted = distillWithRegex(messages);
        }

        if (isEmptyProfile(extracted)) {
            log.debug("No meaningful preferences extracted: userId={}", userId);
            return;
        }

        UserProfile existing = findProfileByUserId(userId);
        if (existing == null) {
            createNewProfile(userId, sessionId, extracted);
        } else {
            mergeAndUpdateProfile(existing, sessionId, extracted);
        }
    }

    @Override
    public void updatePreference(String userId, String key, String value, double confidence) {
        if (key == null || key.isBlank()) {
            log.warn("Cannot update preference with null/blank key: userId={}", userId);
            return;
        }

        UserProfile profile = findProfileByUserId(userId);
        LocalDateTime now = LocalDateTime.now();

        if (profile == null) {
            JSONObject json = createEmptyProfileJson();
            addOrUpdatePreference(json, key, value, confidence, now);
            createNewProfile(userId, null, json);
        } else {
            JSONObject json = parseProfileJson(profile.getProfileJson());
            addOrUpdatePreference(json, key, value, confidence, now);
            saveProfileWithRetry(profile, json.toJSONString(), null);
        }
    }

    @Override
    public void removePreference(String userId, String key) {
        if (key == null) {
            return;
        }

        UserProfile profile = findProfileByUserId(userId);
        if (profile == null) {
            return;
        }

        JSONObject json = parseProfileJson(profile.getProfileJson());
        JSONArray prefs = json.getJSONArray("preferences");
        if (prefs == null || prefs.isEmpty()) {
            return;
        }

        int originalSize = prefs.size();
        prefs.removeIf(p -> key.equals(((JSONObject) p).getString("key")));

        if (prefs.size() < originalSize) {
            saveProfileWithRetry(profile, json.toJSONString(), null);
        }
    }

    @Override
    public Map<String, String> getPreferences(String userId) {
        UserProfile profile = getProfile(userId);
        if (profile == null || profile.getProfileJson() == null) {
            return Collections.emptyMap();
        }

        JSONObject json = parseProfileJson(profile.getProfileJson());
        JSONArray prefs = json.getJSONArray("preferences");
        if (prefs == null) {
            return Collections.emptyMap();
        }

        Map<String, String> result = new LinkedHashMap<>();
        for (int i = 0; i < prefs.size(); i++) {
            JSONObject pref = prefs.getJSONObject(i);
            String prefKey = pref.getString("key");
            String prefValue = pref.getString("value");
            if (prefKey != null && prefValue != null) {
                result.put(prefKey, prefValue);
            }
        }
        return result;
    }

    @Override
    public void enforceMemoryLimit(String userId) {
        UserProfile profile = findProfileByUserId(userId);
        if (profile == null || profile.getProfileJson() == null) {
            return;
        }

        int maxProfileSize = memoryConfig.getMaxProfileSize();
        if (profile.getProfileJson().length() <= maxProfileSize) {
            return;
        }

        log.info("Profile exceeds {} chars, consolidating: userId={}, currentSize={}",
                maxProfileSize, userId, profile.getProfileJson().length());

        String consolidated = consolidateWithLlm(profile.getProfileJson());
        if (consolidated != null && consolidated.length() <= maxProfileSize) {
            saveProfileWithRetry(profile, consolidated, null);
            return;
        }

        // Fallback: truncate low-confidence preferences
        String truncated = truncateLowConfidence(profile.getProfileJson());
        saveProfileWithRetry(profile, truncated, null);
    }

    // ================================================================
    // Scheduled cleanup
    // ================================================================

    /**
     * Scheduled cleanup job that scans all user profiles and removes
     * expired preferences whose confidence has decayed below the threshold.
     *
     * Profiles that become completely empty after cleanup are deleted
     * to avoid accumulating stale data.
     *
     * Runs at a configurable interval defined by travel.memory.cleanup-interval-minutes.
     */
    @Scheduled(fixedRateString = "${travel.memory.cleanup-interval-minutes:60}000")
    public void cleanupExpiredPreferences() {
        log.debug("Starting scheduled profile cleanup");
        int profilesScanned = 0;
        int profilesUpdated = 0;
        int profilesDeleted = 0;
        int preferencesRemoved = 0;

        try {
            List<UserProfile> allProfiles = userProfileMapper.selectList(new QueryWrapper<>());
            if (allProfiles == null || allProfiles.isEmpty()) {
                log.debug("No profiles to clean up");
                return;
            }

            profilesScanned = allProfiles.size();

            for (UserProfile profile : allProfiles) {
                try {
                    String originalJson = profile.getProfileJson();
                    if (originalJson == null || originalJson.isBlank()) {
                        continue;
                    }

                    String decayedJson = applyTimeDecay(originalJson);
                    JSONObject decayed = parseProfileJson(decayedJson);

                    boolean changed = !originalJson.equals(decayedJson);
                    boolean isEmpty = isEmptyProfile(decayed);

                    if (isEmpty) {
                        userProfileMapper.deleteById(profile.getId());
                        profilesDeleted++;
                        log.debug("Deleted empty profile: userId={}", profile.getUserId());
                    } else if (changed) {
                        int prefsBefore = countPreferences(originalJson);
                        int prefsAfter = countPreferences(decayedJson);
                        preferencesRemoved += (prefsBefore - prefsAfter);
                        saveProfileWithRetry(profile, decayedJson, null);
                        profilesUpdated++;
                    }
                } catch (Exception e) {
                    log.warn("Failed to clean up profile userId={}: {}",
                            profile.getUserId(), e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("Profile cleanup failed: {}", e.getMessage());
        }

        if (profilesUpdated > 0 || profilesDeleted > 0) {
            log.info("Profile cleanup complete: scanned={}, updated={}, deleted={}, preferencesRemoved={}",
                    profilesScanned, profilesUpdated, profilesDeleted, preferencesRemoved);
        }
    }

    /**
     * Count the number of preferences in a profile JSON string.
     */
    private int countPreferences(String profileJson) {
        JSONObject json = parseProfileJson(profileJson);
        JSONArray prefs = json.getJSONArray("preferences");
        return prefs == null ? 0 : prefs.size();
    }

    // ================================================================
    // Distillation: LLM-based extraction
    // ================================================================

    private JSONObject distillWithLlm(List<Map<String, String>> messages) {
        String conversationText = formatMessagesForLlm(messages);
        String prompt = String.format(DISTILLATION_PROMPT_TEMPLATE, conversationText);

        String response = chatLanguageModel.generate(prompt);
        if (response == null || response.isBlank()) {
            log.warn("LLM returned empty response for distillation");
            return null;
        }

        return parseLlmJsonResponse(response);
    }

    private JSONObject parseLlmJsonResponse(String response) {
        // Try to extract JSON from markdown code blocks or raw response
        String jsonStr = response.trim();
        if (jsonStr.contains("```json")) {
            int start = jsonStr.indexOf("```json") + 7;
            int end = jsonStr.indexOf("```", start);
            if (end > start) {
                jsonStr = jsonStr.substring(start, end).trim();
            }
        } else if (jsonStr.contains("```")) {
            int start = jsonStr.indexOf("```") + 3;
            int end = jsonStr.indexOf("```", start);
            if (end > start) {
                jsonStr = jsonStr.substring(start, end).trim();
            }
        }

        try {
            return JSON.parseObject(jsonStr);
        } catch (Exception e) {
            log.warn("Failed to parse LLM JSON response: {}", e.getMessage());
            return null;
        }
    }

    // ================================================================
    // Distillation: Regex-based fallback extraction
    // ================================================================

    JSONObject distillWithRegex(List<Map<String, String>> messages) {
        JSONObject profile = createEmptyProfileJson();
        String allText = formatMessagesForLlm(messages);

        // Extract budget
        extractBudget(profile, allText);

        // Extract destinations
        extractDestinations(profile, allText);

        // Extract dietary preferences
        extractDietary(profile, allText);

        // Extract companion info
        extractCompanion(profile, allText);

        return profile;
    }

    private void extractBudget(JSONObject profile, String text) {
        Matcher matcher = BUDGET_PATTERN.matcher(text);
        if (matcher.find()) {
            String low = matcher.group(1);
            String high = matcher.group(3);
            String budgetValue = (high != null) ? low + "-" + high : low;
            addOrUpdatePreference(profile, "budget_range", budgetValue, 0.7, LocalDateTime.now());
        }
    }

    private void extractDestinations(JSONObject profile, String text) {
        JSONObject history = profile.getJSONObject("history");
        if (history == null) {
            history = new JSONObject();
            profile.put("history", history);
        }
        JSONArray interested = history.getJSONArray("interested");
        if (interested == null) {
            interested = new JSONArray();
            history.put("interested", interested);
        }

        Set<String> found = new LinkedHashSet<>();
        for (String dest : KNOWN_DESTINATIONS) {
            if (text.contains(dest)) {
                found.add(dest);
            }
        }
        for (String dest : found) {
            if (!interested.contains(dest)) {
                interested.add(dest);
            }
        }
    }

    private void extractDietary(JSONObject profile, String text) {
        Matcher matcher = DIETARY_PATTERN.matcher(text);
        if (matcher.find()) {
            String dietary = matcher.group(1);
            JSONObject demographics = profile.getJSONObject("demographics");
            if (demographics == null) {
                demographics = new JSONObject();
                profile.put("demographics", demographics);
            }
            demographics.put("dietary", dietary);
        }
    }

    private void extractCompanion(JSONObject profile, String text) {
        Matcher matcher = COMPANION_PATTERN.matcher(text);
        if (matcher.find()) {
            String companion = matcher.group(1);
            JSONObject demographics = profile.getJSONObject("demographics");
            if (demographics == null) {
                demographics = new JSONObject();
                profile.put("demographics", demographics);
            }
            demographics.put("travel_companion", companion);
        }
    }

    // ================================================================
    // Profile merging and persistence
    // ================================================================

    private void mergeAndUpdateProfile(UserProfile existing, String sessionId, JSONObject newData) {
        JSONObject existingJson = parseProfileJson(existing.getProfileJson());
        JSONObject merged = mergeProfiles(existingJson, newData);
        saveProfileWithRetry(existing, merged.toJSONString(), sessionId);
    }

    JSONObject mergeProfiles(JSONObject existing, JSONObject newData) {
        // Merge preferences
        JSONArray existingPrefs = existing.getJSONArray("preferences");
        if (existingPrefs == null) {
            existingPrefs = new JSONArray();
            existing.put("preferences", existingPrefs);
        }
        JSONArray newPrefs = newData.getJSONArray("preferences");
        if (newPrefs != null) {
            for (int i = 0; i < newPrefs.size(); i++) {
                JSONObject newPref = newPrefs.getJSONObject(i);
                addOrUpdatePreference(existingPrefs, newPref);
            }
        }

        // Merge demographics
        JSONObject existingDemo = existing.getJSONObject("demographics");
        JSONObject newDemo = newData.getJSONObject("demographics");
        if (newDemo != null) {
            if (existingDemo == null) {
                existingDemo = new JSONObject();
                existing.put("demographics", existingDemo);
            }
            existingDemo.putAll(newDemo);
        }

        // Merge history
        mergeHistory(existing, newData);

        // Merge feedback
        JSONArray existingFeedback = existing.getJSONArray("feedback");
        if (existingFeedback == null) {
            existingFeedback = new JSONArray();
            existing.put("feedback", existingFeedback);
        }
        JSONArray newFeedback = newData.getJSONArray("feedback");
        if (newFeedback != null) {
            existingFeedback.addAll(newFeedback);
        }

        return existing;
    }

    private void mergeHistory(JSONObject existing, JSONObject newData) {
        JSONObject existingHistory = existing.getJSONObject("history");
        JSONObject newHistory = newData.getJSONObject("history");
        if (newHistory == null) {
            return;
        }
        if (existingHistory == null) {
            existingHistory = new JSONObject();
            existing.put("history", existingHistory);
        }

        // Merge "interested" arrays with deduplication
        JSONArray existingInterested = existingHistory.getJSONArray("interested");
        JSONArray newInterested = newHistory.getJSONArray("interested");
        if (newInterested != null) {
            if (existingInterested == null) {
                existingInterested = new JSONArray();
                existingHistory.put("interested", existingInterested);
            }
            Set<Object> existingSet = new LinkedHashSet<>(existingInterested);
            for (int i = 0; i < newInterested.size(); i++) {
                Object item = newInterested.get(i);
                if (!existingSet.contains(item)) {
                    existingInterested.add(item);
                    existingSet.add(item);
                }
            }
        }

        // Merge "visited" arrays with deduplication
        JSONArray existingVisited = existingHistory.getJSONArray("visited");
        JSONArray newVisited = newHistory.getJSONArray("visited");
        if (newVisited != null) {
            if (existingVisited == null) {
                existingVisited = new JSONArray();
                existingHistory.put("visited", existingVisited);
            }
            Set<Object> existingSet = new LinkedHashSet<>(existingVisited);
            for (int i = 0; i < newVisited.size(); i++) {
                Object item = newVisited.get(i);
                if (!existingSet.contains(item)) {
                    existingVisited.add(item);
                    existingSet.add(item);
                }
            }
        }
    }

    private void addOrUpdatePreference(JSONArray prefs, JSONObject newPref) {
        String key = newPref.getString("key");
        if (key == null) {
            return;
        }

        for (int i = 0; i < prefs.size(); i++) {
            JSONObject existing = prefs.getJSONObject(i);
            if (key.equals(existing.getString("key"))) {
                // Take the higher confidence value
                double existingConf = existing.containsKey("confidence") ? existing.getDoubleValue("confidence") : 0.0;
                double newConf = newPref.containsKey("confidence") ? newPref.getDoubleValue("confidence") : 0.0;
                if (newConf >= existingConf) {
                    existing.put("value", newPref.getString("value"));
                    existing.put("confidence", newConf);
                    existing.put("updatedAt", newPref.getString("updatedAt"));
                }
                return;
            }
        }
        // Key not found, add new
        prefs.add(newPref);
    }

    private void addOrUpdatePreference(JSONObject profile, String key, String value,
                                        double confidence, LocalDateTime updatedAt) {
        JSONArray prefs = profile.getJSONArray("preferences");
        if (prefs == null) {
            prefs = new JSONArray();
            profile.put("preferences", prefs);
        }

        for (int i = 0; i < prefs.size(); i++) {
            JSONObject pref = prefs.getJSONObject(i);
            if (key.equals(pref.getString("key"))) {
                double existingConf = getConfidence(pref);
                if (confidence >= existingConf) {
                    pref.put("value", value);
                    pref.put("confidence", confidence);
                    pref.put("updatedAt", updatedAt.toString());
                }
                return;
            }
        }

        JSONObject newPref = new JSONObject();
        newPref.put("key", key);
        newPref.put("value", value);
        newPref.put("confidence", confidence);
        newPref.put("updatedAt", updatedAt.toString());
        prefs.add(newPref);
    }

    // ================================================================
    // Time decay
    // ================================================================

    String applyTimeDecay(String profileJson) {
        if (profileJson == null || profileJson.isBlank()) {
            return profileJson;
        }

        JSONObject json = parseProfileJson(profileJson);
        JSONArray prefs = json.getJSONArray("preferences");
        if (prefs == null || prefs.isEmpty()) {
            return json.toJSONString();
        }

        double decayRate = memoryConfig.getDecayRate();
        double minConfidence = memoryConfig.getMinConfidence();

        LocalDateTime now = LocalDateTime.now();
        List<JSONObject> toRemove = new ArrayList<>();

        for (int i = 0; i < prefs.size(); i++) {
            JSONObject pref = prefs.getJSONObject(i);
            double confidence = getConfidence(pref);
            String updatedAtStr = pref.getString("updatedAt");

            if (updatedAtStr != null) {
                try {
                    LocalDateTime updatedAt = LocalDateTime.parse(updatedAtStr);
                    long daysSinceUpdate = ChronoUnit.DAYS.between(updatedAt, now);
                    double decayedConfidence = confidence * Math.exp(-decayRate * daysSinceUpdate);
                    pref.put("confidence", Math.round(decayedConfidence * 10000.0) / 10000.0);

                    if (decayedConfidence < minConfidence) {
                        toRemove.add(pref);
                    }
                } catch (Exception e) {
                    log.debug("Failed to parse updatedAt for decay: {}", updatedAtStr);
                }
            }
        }

        if (!toRemove.isEmpty()) {
            prefs.removeAll(toRemove);
            log.debug("Removed {} preferences below threshold after decay", toRemove.size());
        }

        return json.toJSONString();
    }

    // ================================================================
    // Bounded memory: consolidation
    // ================================================================

    private String consolidateWithLlm(String profileJson) {
        try {
            String prompt = String.format(CONSOLIDATION_PROMPT_TEMPLATE, profileJson);
            String response = chatLanguageModel.generate(prompt);
            if (response == null || response.isBlank()) {
                return null;
            }
            // Validate the response is valid JSON
            JSONObject parsed = parseLlmJsonResponse(response);
            if (parsed != null) {
                return parsed.toJSONString();
            }
            return null;
        } catch (Exception e) {
            log.warn("LLM consolidation failed: {}", e.getMessage());
            return null;
        }
    }

    private String truncateLowConfidence(String profileJson) {
        JSONObject json = parseProfileJson(profileJson);
        JSONArray prefs = json.getJSONArray("preferences");
        if (prefs == null || prefs.isEmpty()) {
            return json.toJSONString();
        }

        int maxProfileSize = memoryConfig.getMaxProfileSize();

        // Sort by confidence ascending and remove until under limit
        List<JSONObject> prefList = new ArrayList<>();
        for (int i = 0; i < prefs.size(); i++) {
            prefList.add(prefs.getJSONObject(i));
        }
        prefList.sort(Comparator.comparingDouble(this::getConfidence));

        // Remove lowest confidence items until under limit
        Iterator<JSONObject> it = prefList.iterator();
        while (it.hasNext() && json.toJSONString().length() > maxProfileSize) {
            JSONObject toRemove = it.next();
            prefs.remove(toRemove);
        }

        return json.toJSONString();
    }

    // ================================================================
    // Profile context formatting
    // ================================================================

    private String formatProfileAsContext(String profileJson) {
        JSONObject json = parseProfileJson(profileJson);
        StringBuilder sb = new StringBuilder();
        sb.append("【用户画像信息】\n");

        // Format preferences
        JSONArray prefs = json.getJSONArray("preferences");
        if (prefs != null && !prefs.isEmpty()) {
            sb.append("偏好：\n");
            for (int i = 0; i < prefs.size(); i++) {
                JSONObject pref = prefs.getJSONObject(i);
                sb.append("  - ").append(pref.getString("key"))
                  .append(": ").append(pref.getString("value"))
                  .append(" (置信度: ").append(String.format("%.0f%%",
                          getConfidence(pref) * 100))
                  .append(")\n");
            }
        }

        // Format demographics
        JSONObject demographics = json.getJSONObject("demographics");
        if (demographics != null && !demographics.isEmpty()) {
            sb.append("基本信息：\n");
            for (Map.Entry<String, Object> entry : demographics.entrySet()) {
                sb.append("  - ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
            }
        }

        // Format history
        JSONObject history = json.getJSONObject("history");
        if (history != null && !history.isEmpty()) {
            JSONArray visited = history.getJSONArray("visited");
            if (visited != null && !visited.isEmpty()) {
                sb.append("去过的地方：").append(visited).append("\n");
            }
            JSONArray interested = history.getJSONArray("interested");
            if (interested != null && !interested.isEmpty()) {
                sb.append("感兴趣的地方：").append(interested).append("\n");
            }
        }

        // Format feedback
        JSONArray feedback = json.getJSONArray("feedback");
        if (feedback != null && !feedback.isEmpty()) {
            sb.append("反馈记录：\n");
            for (int i = 0; i < feedback.size(); i++) {
                JSONObject fb = feedback.getJSONObject(i);
                sb.append("  - ").append(fb.getString("topic"))
                  .append(" (").append(fb.getString("sentiment")).append(")")
                  .append(": ").append(fb.getString("note")).append("\n");
            }
        }

        return sb.toString();
    }

    // ================================================================
    // Persistence helpers
    // ================================================================

    private void createNewProfile(String userId, String sessionId, JSONObject profileJson) {
        UserProfile profile = new UserProfile();
        profile.setUserId(userId);
        profile.setProfileJson(profileJson.toJSONString());
        profile.setSourceCount(1);
        profile.setLastConversationId(sessionId);
        profile.setCreatedAt(LocalDateTime.now());
        profile.setUpdatedAt(LocalDateTime.now());
        profile.setVersion(1);

        userProfileMapper.insert(profile);
        log.info("Created new user profile: userId={}", userId);
    }

    private void saveProfileWithRetry(UserProfile existing, String newJson, String sessionId) {
        existing.setProfileJson(newJson);
        existing.setUpdatedAt(LocalDateTime.now());
        if (sessionId != null) {
            existing.setLastConversationId(sessionId);
            existing.setSourceCount(
                    (existing.getSourceCount() != null ? existing.getSourceCount() : 0) + 1
            );
        }

        for (int attempt = 0; attempt < MAX_OPTIMISTIC_LOCK_RETRIES; attempt++) {
            int rows = userProfileMapper.updateById(existing);
            if (rows > 0) {
                return;
            }
            // Optimistic lock failure: reload and retry
            log.warn("Optimistic lock failure, retrying (attempt {}): userId={}", attempt + 1, existing.getUserId());
            UserProfile fresh = findProfileByUserId(existing.getUserId());
            if (fresh == null) {
                log.error("Profile disappeared during optimistic lock retry: userId={}", existing.getUserId());
                return;
            }
            existing.setId(fresh.getId());
            existing.setVersion(fresh.getVersion());
        }
        log.error("Failed to save profile after {} retries: userId={}", MAX_OPTIMISTIC_LOCK_RETRIES, existing.getUserId());
    }

    // ================================================================
    // Utility methods
    // ================================================================

    private UserProfile findProfileByUserId(String userId) {
        QueryWrapper<UserProfile> query = new QueryWrapper<>();
        query.eq("user_id", userId);
        return userProfileMapper.selectOne(query);
    }

    private JSONObject parseProfileJson(String profileJson) {
        if (profileJson == null || profileJson.isBlank()) {
            return createEmptyProfileJson();
        }
        try {
            return JSON.parseObject(profileJson);
        } catch (Exception e) {
            log.warn("Failed to parse profileJson, returning empty: {}", e.getMessage());
            return createEmptyProfileJson();
        }
    }

    private JSONObject createEmptyProfileJson() {
        JSONObject json = new JSONObject();
        json.put("preferences", new JSONArray());
        json.put("demographics", new JSONObject());
        json.put("history", new JSONObject());
        json.put("feedback", new JSONArray());
        return json;
    }

    private boolean isEmptyProfile(JSONObject profile) {
        if (profile == null) {
            return true;
        }
        JSONArray prefs = profile.getJSONArray("preferences");
        JSONObject demo = profile.getJSONObject("demographics");
        JSONObject history = profile.getJSONObject("history");
        JSONArray feedback = profile.getJSONArray("feedback");

        boolean hasPrefs = prefs != null && !prefs.isEmpty();
        boolean hasDemo = demo != null && !demo.isEmpty();
        boolean hasHistory = history != null && !history.isEmpty();
        boolean hasFeedback = feedback != null && !feedback.isEmpty();

        return !hasPrefs && !hasDemo && !hasHistory && !hasFeedback;
    }

    private String formatMessagesForLlm(List<Map<String, String>> messages) {
        StringBuilder sb = new StringBuilder();
        for (Map<String, String> msg : messages) {
            String role = msg.getOrDefault("role", "unknown");
            String content = msg.getOrDefault("content", "");
            String roleName = "user".equals(role) ? "用户" : "助手";
            sb.append(roleName).append(": ").append(content).append("\n");
        }
        return sb.toString();
    }

    /**
     * Safely get confidence from a preference JSONObject.
     * Returns 0.0 if the key is missing or the value is not a number.
     */
    private double getConfidence(JSONObject pref) {
        if (pref == null || !pref.containsKey("confidence")) {
            return 0.0;
        }
        return pref.getDoubleValue("confidence");
    }
}
