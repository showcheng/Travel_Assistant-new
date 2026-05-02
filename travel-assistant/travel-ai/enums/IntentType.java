package com.travel.ai.enums;

/**
 * 意图类型枚举
 * 定义AI助手支持的所有意图类型
 */
public enum IntentType {

    /**
     * 问候类意图
     */
    GREETING("greeting", "问候"),

    /**
     * 产品推荐意图
     */
    PRODUCT_RECOMMENDATION("product_recommendation", "产品推荐"),

    /**
     * 订单查询意图
     */
    ORDER_QUERY("order_query", "订单查询"),

    /**
     * 价格询问意图
     */
    PRICE_INQUIRY("price_inquiry", "价格询问"),

    /**
     * 景点查询意图
     */
    ATTRACTION_QUERY("attraction_query", "景点查询"),

    /**
     * 退改政策询问
     */
    POLICY_INQUIRY("policy_inquiry", "政策询问"),

    /**
     * 支付相关
     */
    PAYMENT_INQUIRY("payment_inquiry", "支付询问"),

    /**
     * 通用对话
     */
    GENERAL("general", "通用对话"),

    /**
     * 未知意图
     */
    UNKNOWN("unknown", "未知");

    private final String code;
    private final String description;

    IntentType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 根据代码获取意图类型
     */
    public static IntentType fromCode(String code) {
        for (IntentType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        return UNKNOWN;
    }
}
