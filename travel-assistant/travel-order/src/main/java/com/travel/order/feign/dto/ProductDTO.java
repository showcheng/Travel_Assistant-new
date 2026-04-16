package com.travel.order.feign.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 商品数据传输对象
 */
@Data
public class ProductDTO {

    /**
     * 商品ID
     */
    private Long id;

    /**
     * 商品名称
     */
    private String name;

    /**
     * 商品分类ID
     */
    private Long categoryId;

    /**
     * 商品类型：1-门票，2-商品
     */
    private Integer type;

    /**
     * 现价
     */
    private BigDecimal price;

    /**
     * 原价
     */
    private BigDecimal originalPrice;

    /**
     * 库存数量
     */
    private Integer stock;

    /**
     * 销量
     */
    private Integer sales;

    /**
     * 商品图片URL
     */
    private String coverImage;

    /**
     * 商品详情
     */
    private String detail;

    /**
     * 商品状态：0-下架，1-上架
     */
    private Integer status;

    /**
     * 别名方法，用于兼容不同字段名
     */
    public String getProductName() {
        return name;
    }

    public String getProductImage() {
        return coverImage;
    }

    public BigDecimal getCurrentPrice() {
        return price;
    }
}