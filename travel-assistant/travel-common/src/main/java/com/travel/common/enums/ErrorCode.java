package com.travel.common.enums;

import lombok.Getter;

/**
 * 错误码枚举
 */
@Getter
public enum ErrorCode {

    // 通用错误码 (1xxx)
    SUCCESS(200, "操作成功"),
    ERROR(500, "系统异常"),
    PARAM_ERROR(400, "参数错误"),
    NOT_FOUND(404, "资源不存在"),
    UNAUTHORIZED(401, "未认证"),
    FORBIDDEN(403, "无权限"),
    METHOD_NOT_ALLOWED(405, "请求方法不支持"),

    // 用户相关 (10xxx)
    USER_NOT_FOUND(10001, "用户不存在"),
    USER_ALREADY_EXISTS(10002, "用户已存在"),
    USER_PASSWORD_ERROR(10003, "密码错误"),
    USER_ACCOUNT_LOCKED(10004, "账号已锁定"),
    USER_TOKEN_EXPIRED(10005, "Token 已过期"),
    USER_TOKEN_INVALID(10006, "Token 无效"),
    USER_SMS_CODE_ERROR(10007, "验证码错误"),
    USER_SMS_CODE_EXPIRED(10008, "验证码已过期"),

    // 商品相关 (20xxx)
    PRODUCT_NOT_FOUND(20001, "商品不存在"),
    PRODUCT_OUT_OF_STOCK(20002, "库存不足"),
    PRODUCT_SOLD_OUT(20003, "商品已售罄"),
    PRODUCT_OFFLINE(20004, "商品已下架"),
    INVALID_PRODUCT_TYPE(20005, "商品类型无效"),
    INVALID_PRICE(20006, "价格无效"),
    PRODUCT_STOCK_UPDATE_FAILED(20007, "库存更新失败，请重试"),

    // 订单相关 (30xxx)
    ORDER_NOT_FOUND(30001, "订单不存在"),
    ORDER_EXPIRED(30002, "订单已过期"),
    ORDER_ALREADY_PAID(30003, "订单已支付"),
    ORDER_CANCEL_FAILED(30004, "订单取消失败"),
    ORDER_REFUND_FAILED(30005, "订单退款失败"),
    ORDER_STATUS_ERROR(30006, "订单状态错误"),

    // 秒杀相关 (40xxx)
    SECKILL_NOT_STARTED(40001, "秒杀未开始"),
    SECKILL_ALREADY_ENDED(40002, "秒杀已结束"),
    SECKILL_OUT_OF_STOCK(40003, "秒杀商品已抢完"),
    SECKILL_REPEAT_PURCHASE(40004, "重复抢购"),
    SECKILL_VERIFY_FAILED(40005, "验证失败"),
    SECKILL_LIMIT_EXCEEDED(40006, "超过抢购限制"),

    // 拼团相关 (50xxx)
    GROUP_BUY_NOT_FOUND(50001, "拼团不存在"),
    GROUP_BUY_ALREADY_COMPLETED(50002, "拼团已成团"),
    GROUP_BUY_ALREADY_CANCELLED(50003, "拼团已取消"),
    GROUP_BUY_MEMBER_LIMIT(50004, "拼团人数已满"),
    GROUP_BUY_EXPIRED(50005, "拼团已过期"),

    // AI 相关 (60xxx)
    AI_SERVICE_ERROR(60001, "AI 服务异常"),
    AI_REQUEST_FAILED(60002, "AI 请求失败"),
    AI_RESPONSE_PARSE_ERROR(60003, "AI 响应解析失败"),
    AI_RATE_LIMIT_EXCEEDED(60004, "AI 请求超限"),

    // 支付相关 (70xxx)
    PAYMENT_FAILED(70001, "支付失败"),
    PAYMENT_CALLBACK_ERROR(70002, "支付回调异常"),
    PAYMENT_NOT_FOUND(70003, "支付记录不存在"),
    PAYMENT_ALREADY_REFUNDED(70004, "已退款"),
    PAYMENT_REFUND_FAILED(70005, "退款失败"),
    PAYMENT_ALREADY_COMPLETED(70006, "支付已完成");

    private final Integer code;
    private final String message;

    ErrorCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}
