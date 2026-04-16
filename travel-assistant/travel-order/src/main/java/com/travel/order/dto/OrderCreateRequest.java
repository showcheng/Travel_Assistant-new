package com.travel.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.List;

/**
 * 订单创建请求
 */
@Data
@Schema(description = "订单创建请求")
public class OrderCreateRequest {

    @Schema(description = "订单明细列表")
    @NotEmpty(message = "订单明细不能为空")
    @Size(min = 1, max = 50, message = "订单明细数量必须在1-50之间")
    private List<OrderItemCreateRequest> items;

    @Schema(description = "备注")
    @Size(max = 500, message = "备注长度不能超过500个字符")
    private String remark;

    /**
     * 兼容旧的单商品创建方式
     * 用于向后兼容
     */
    @Deprecated
    public Long getProductId() {
        return items != null && !items.isEmpty() ? items.get(0).getProductId() : null;
    }

    @Deprecated
    public Integer getQuantity() {
        return items != null && !items.isEmpty() ? items.get(0).getQuantity() : null;
    }
}
