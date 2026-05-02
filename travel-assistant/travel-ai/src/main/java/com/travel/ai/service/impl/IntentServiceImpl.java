package com.travel.ai.service.impl;

import com.travel.ai.enums.IntentType;
import com.travel.ai.service.IntentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 意图识别服务实现类
 * 使用基于规则的识别方法，支持关键词匹配和实体提取
 */
@Slf4j
@Service
public class IntentServiceImpl implements IntentService {

    // 意图关键词模式
    private static final Map<IntentType, List<Pattern>> INTENT_PATTERNS = new EnumMap<>(IntentType.class);

    // 实体提取模式
    private static final Pattern LOCATION_PATTERN = Pattern.compile("(北京|上海|西安|成都|杭州|南京|广州|深圳)(市|)?");
    private static final Pattern PRICE_PATTERN = Pattern.compile("(\\d+)元|(\\d+)块");
    private static final Pattern NUMBER_PATTERN = Pattern.compile("(\\d+)个|(\\d+)个");
    private static final Pattern DATE_PATTERN = Pattern.compile("(今天|明天|后天|本周|下周|本月|下月)");

    static {
        // 问候类意图
        INTENT_PATTERNS.put(IntentType.GREETING, Arrays.asList(
            Pattern.compile("你好|您好|嗨|hello|hi|早上好|晚上好"),
            Pattern.compile("在吗|在不在|有人吗")
        ));

        // 产品推荐意图
        INTENT_PATTERNS.put(IntentType.PRODUCT_RECOMMENDATION, Arrays.asList(
            Pattern.compile("推荐|介绍|建议|有什么.*?景点|有什么.*?产品"),
            Pattern.compile("想去|打算|计划.*?旅游|去.*?玩"),
            Pattern.compile("好玩的地方|值得去的地方|特色景点")
        ));

        // 订单查询意图
        INTENT_PATTERNS.put(IntentType.ORDER_QUERY, Arrays.asList(
            Pattern.compile("订单|我的订单|查询订单|订单查询"),
            Pattern.compile("买了什么|买过什么|购买记录"),
            Pattern.compile("订单状态|订单详情|我的.*?订单")
        ));

        // 价格询问意图
        INTENT_PATTERNS.put(IntentType.PRICE_INQUIRY, Arrays.asList(
            Pattern.compile("多少钱|价格|费用|成本|票价"),
            Pattern.compile("便宜|贵|性价比|折扣|优惠")
        ));

        // 景点查询意图
        INTENT_PATTERNS.put(IntentType.ATTRACTION_QUERY, Arrays.asList(
            Pattern.compile("景点|景区|地方|好玩|值得去"),
            Pattern.compile("介绍一下|怎么样|如何|特色")
        ));

        // 政策询问意图
        INTENT_PATTERNS.put(IntentType.POLICY_INQUIRY, Arrays.asList(
            Pattern.compile("退票|退款|改签|政策|规则|规定"),
            Pattern.compile("能不能退|可以退|怎么退|退票流程")
        ));

        // 支付询问意图
        INTENT_PATTERNS.put(IntentType.PAYMENT_INQUIRY, Arrays.asList(
            Pattern.compile("支付|付款|结算|买单|怎么付"),
            Pattern.compile("支付宝|微信支付|银行卡|现金")
        ));
    }

    @Override
    public IntentType recognizeIntent(String message) {
        if (message == null || message.trim().isEmpty()) {
            return IntentType.UNKNOWN;
        }

        String lowerMessage = message.toLowerCase();

        // 遍历所有意图类型
        for (Map.Entry<IntentType, List<Pattern>> entry : INTENT_PATTERNS.entrySet()) {
            IntentType intentType = entry.getKey();
            List<Pattern> patterns = entry.getValue();

            // 检查是否匹配任何模式
            for (Pattern pattern : patterns) {
                Matcher matcher = pattern.matcher(lowerMessage);
                if (matcher.find()) {
                    log.debug("识别意图: message={}, intent={}", message, intentType);
                    return intentType;
                }
            }
        }

        // 未匹配到特定意图，返回通用对话
        log.debug("未识别到特定意图，返回通用对话: message={}", message);
        return IntentType.GENERAL;
    }

    @Override
    public Map<String, Object> extractEntities(String message, IntentType intent) {
        Map<String, Object> entities = new HashMap<>();

        try {
            // 提取地点
            Matcher locationMatcher = LOCATION_PATTERN.matcher(message);
            if (locationMatcher.find()) {
                entities.put("location", locationMatcher.group());
            }

            // 提取价格
            Matcher priceMatcher = PRICE_PATTERN.matcher(message);
            if (priceMatcher.find()) {
                entities.put("price", priceMatcher.group(1));
            }

            // 提取数量
            Matcher numberMatcher = NUMBER_PATTERN.matcher(message);
            if (numberMatcher.find()) {
                entities.put("count", Integer.parseInt(numberMatcher.group(1)));
            }

            // 提取时间
            Matcher dateMatcher = DATE_PATTERN.matcher(message);
            if (dateMatcher.find()) {
                entities.put("time", dateMatcher.group());
            }

            // 根据意图类型提取特定实体
            switch (intent) {
                case PRODUCT_RECOMMENDATION:
                    extractRecommendationEntities(message, entities);
                    break;
                case ORDER_QUERY:
                    extractOrderEntities(message, entities);
                    break;
                case PRICE_INQUIRY:
                    extractPriceEntities(message, entities);
                    break;
                default:
                    break;
            }

            log.debug("提取实体: message={}, entities={}", message, entities);

        } catch (Exception e) {
            log.error("提取实体失败: message={}", message, e);
        }

        return entities;
    }

    @Override
    public IntentType analyzeWithContext(String message, String context) {
        // 如果有上下文，优先考虑上下文相关的意图
        if (context != null && !context.isEmpty()) {
            // 检查是否是追问
            if (isFollowUpQuestion(message)) {
                // 返回与上下文相关的意图
                if (context.contains("订单")) {
                    return IntentType.ORDER_QUERY;
                } else if (context.contains("景点") || context.contains("产品")) {
                    return IntentType.PRODUCT_RECOMMENDATION;
                }
            }
        }

        // 否则使用正常的意图识别
        return recognizeIntent(message);
    }

    @Override
    public String generateRoute(IntentType intent, Map<String, Object> entities) {
        switch (intent) {
            case PRODUCT_RECOMMENDATION:
                return "/recommend/products";
            case ORDER_QUERY:
                return "/orders/query";
            case PRICE_INQUIRY:
                return "/products/price";
            case ATTRACTION_QUERY:
                return "/attractions/info";
            case POLICY_INQUIRY:
                return "/policy/info";
            case PAYMENT_INQUIRY:
                return "/payment/info";
            case GREETING:
                return "/greeting";
            case GENERAL:
                return "/chat";
            default:
                return "/chat";
        }
    }

    @Override
    public boolean requiresExternalService(IntentType intent) {
        // 需要调用外部服务的意图类型
        return intent == IntentType.PRODUCT_RECOMMENDATION
            || intent == IntentType.ORDER_QUERY
            || intent == IntentType.ATTRACTION_QUERY;
    }

    /**
     * 提取推荐相关实体
     */
    private void extractRecommendationEntities(String message, Map<String, Object> entities) {
        // 提取目标人群
        if (message.contains("老人") || message.contains("老年人") || message.contains("长辈")) {
            entities.put("targetAudience", "老人");
        } else if (message.contains("孩子") || message.contains("小朋友") || message.contains("儿童")) {
            entities.put("targetAudience", "儿童");
        } else if (message.contains("家庭") || message.contains("全家")) {
            entities.put("targetAudience", "家庭");
        }

        // 提取旅游类型
        if (message.contains("历史") || message.contains("文化")) {
            entities.put("category", "历史文化");
        } else if (message.contains("自然") || message.contains("风景")) {
            entities.put("category", "自然风景");
        } else if (message.contains("美食") || message.contains("小吃")) {
            entities.put("category", "美食");
        }
    }

    /**
     * 提取订单相关实体
     */
    private void extractOrderEntities(String message, Map<String, Object> entities) {
        // 提取订单状态
        if (message.contains("未支付") || message.contains("待支付")) {
            entities.put("orderStatus", "PENDING_PAYMENT");
        } else if (message.contains("已完成") || message.contains("已经完成")) {
            entities.put("orderStatus", "COMPLETED");
        } else if (message.contains("已取消") || message.contains("取消")) {
            entities.put("orderStatus", "CANCELLED");
        }

        // 提取时间范围
        if (message.contains("最近") || message.contains("最近的")) {
            entities.put("timeRange", "RECENT_7_DAYS");
        } else if (message.contains("本月") || message.contains("这个月")) {
            entities.put("timeRange", "THIS_MONTH");
        } else if (message.contains("今年") || message.contains("这一年")) {
            entities.put("timeRange", "THIS_YEAR");
        }
    }

    /**
     * 提取价格相关实体
     */
    private void extractPriceEntities(String message, Map<String, Object> entities) {
        // 提取价格范围
        if (message.contains("便宜") || message.contains("实惠")) {
            entities.put("priceRange", "LOW");
        } else if (message.contains("贵") || message.contains("高档")) {
            entities.put("priceRange", "HIGH");
        } else if (message.contains("中等") || message.contains("一般")) {
            entities.put("priceRange", "MEDIUM");
        }
    }

    /**
     * 检查是否是追问问题
     */
    private boolean isFollowUpQuestion(String message) {
        String[] followUpPatterns = {
            "怎么样", "如何", "怎么", "呢", "吗", "啊", "还有",
            ".*呢", "怎么样", "多少钱", "多少钱"
        };

        for (String pattern : followUpPatterns) {
            if (message.matches(".*" + pattern + ".*")) {
                return true;
            }
        }
        return false;
    }
}
