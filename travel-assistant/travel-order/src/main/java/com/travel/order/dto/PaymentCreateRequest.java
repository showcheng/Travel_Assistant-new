package com.travel.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 创建支付请求
 */
@Data
@Schema(description = "创建支付请求")
public class PaymentCreateRequest {

    @Schema(description = "订单ID")
    @NotNull(message = "订单ID不能为空")
    private Long orderId;

    @Schema(description = "支付金额")
    @NotNull(message = "支付金额不能为空")
    private BigDecimal amount;

    @Schema(description = "支付方式：1-支付宝，2-微信，3-余额")
    @NotNull(message = "支付方式不能为空")
    private Integer payType;
}
