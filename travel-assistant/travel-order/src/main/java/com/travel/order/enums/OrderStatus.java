package com.travel.order.enums;

import lombok.Getter;

/**
 * 订单状态枚举
 */
@Getter
public enum OrderStatus {

    PENDING_PAYMENT(0, "待支付"),
    PAID(1, "已支付"),
    CANCELLED(2, "已取消"),
    REFUNDED(3, "已退款"),
    COMPLETED(4, "已完成");

    private final Integer code;
    private final String description;

    OrderStatus(Integer code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * 根据code获取枚举
     */
    public static OrderStatus getByCode(Integer code) {
        for (OrderStatus status : OrderStatus.values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        return null;
    }

    /**
     * 检查状态是否可以转换为目标状态
     */
    public boolean canTransitionTo(OrderStatus target) {
        switch (this) {
            case PENDING_PAYMENT:
                return target == PAID || target == CANCELLED;
            case PAID:
                return target == REFUNDED || target == COMPLETED;
            case CANCELLED:
            case REFUNDED:
            case COMPLETED:
                return false; // 终态，不能再转换
            default:
                return false;
        }
    }
}
