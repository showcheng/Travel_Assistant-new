package com.travel.product.dto;

import lombok.Data;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

/**
 * 商品创建请求
 */
@Data
public class ProductCreateRequest {

    @NotBlank(message = "商品名称不能为空")
    @Size(max = 100, message = "商品名称长度不能超过100个字符")
    private String name;

    @Size(max = 500, message = "商品描述长度不能超过500个字符")
    private String description;

    @NotBlank(message = "商品类型不能为空")
    private String type;

    @NotNull(message = "原价不能为空")
    @DecimalMin(value = "0.01", message = "原价必须大于0")
    private BigDecimal originalPrice;

    @NotNull(message = "现价不能为空")
    @DecimalMin(value = "0.01", message = "现价必须大于0")
    private BigDecimal currentPrice;

    @NotNull(message = "库存数量不能为空")
    @Min(value = 0, message = "库存数量不能为负数")
    private Integer stock;

    @Size(max = 500, message = "图片URL长度不能超过500个字符")
    private String imageUrl;

    @NotNull(message = "商品状态不能为空")
    private Integer status;

    private Long scenicSpotId;
}
