package com.travel.product.dto;

import lombok.Data;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

/**
 * 商品更新请求
 */
@Data
public class ProductUpdateRequest {

    @NotNull(message = "商品ID不能为空")
    private Long id;

    @Size(max = 100, message = "商品名称长度不能超过100个字符")
    private String name;

    @Size(max = 500, message = "商品描述长度不能超过500个字符")
    private String description;

    private String type;

    @DecimalMin(value = "0.01", message = "原价必须大于0")
    private BigDecimal originalPrice;

    @DecimalMin(value = "0.01", message = "现价必须大于0")
    private BigDecimal currentPrice;

    @Min(value = 0, message = "库存数量不能为负数")
    private Integer stock;

    @Size(max = 500, message = "图片URL长度不能超过500个字符")
    private String imageUrl;

    private Integer status;

    private Long scenicSpotId;
}
