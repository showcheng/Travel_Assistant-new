package com.travel.ai.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 意图类型枚举
 * 定义用户对话意图的分类
 */
@Getter
@AllArgsConstructor
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
     * 政策询问意图
     */
    POLICY_INQUIRY("policy_inquiry", "政策询问"),

    /**
     * 支付询问意图
     */
    PAYMENT_INQUIRY("payment_inquiry", "支付询问"),

    /**
     * 通用对话意图
     */
    GENERAL("general", "通用对话"),

    /**
     * 未知意图
     */
    UNKNOWN("unknown", "未知");

    /**
     * 意图代码
     */
    private final String code;

    /**
     * 意图描述
     */
    private final String description;

    /**
     * 根据代码获取意图类型
     *
     * @param code 意图代码
     * @return 意图类型
     */
    public static IntentType fromCode(String code) {
        for (IntentType type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        return UNKNOWN;
    }
}
