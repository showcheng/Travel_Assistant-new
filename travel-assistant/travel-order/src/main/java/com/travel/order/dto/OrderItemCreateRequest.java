package com.travel.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 订单明细项请求
 */
@Data
@Schema(description = "订单明细项")
public class OrderItemCreateRequest {

    @Schema(description = "商品ID")
    @NotNull(message = "商品ID不能为空")
    private Long productId;

    @Schema(description = "商品数量")
    @NotNull(message = "商品数量不能为空")
    @Min(value = 1, message = "商品数量至少为1")
    @Max(value = 100, message = "商品数量不能超过100")
    private Integer quantity;

    @Schema(description = "商品单价")
    @NotNull(message = "商品单价不能为空")
    private BigDecimal price;

    @Schema(description = "商品名称")
    private String productName;

    @Schema(description = "商品图片")
    private String productImage;
}
