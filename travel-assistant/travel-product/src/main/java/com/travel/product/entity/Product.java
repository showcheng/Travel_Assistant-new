package com.travel.product.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品实体
 */
@Data
@TableName("product")
public class Product {

    @TableId(type = IdType.AUTO)
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
     * 逻辑删除：0-未删除，1-已删除
     */
    private Integer deleted;

    /**
     * 乐观锁版本号
     */
    @Version
    private Integer version;

    /**
     * 创建时间
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    // 为了兼容代码中的字段名，添加getter/setter方法
    public String getProductName() {
        return name;
    }

    public void setProductName(String productName) {
        this.name = productName;
    }

    public BigDecimal getCurrentPrice() {
        return price;
    }

    public void setCurrentPrice(BigDecimal currentPrice) {
        this.price = currentPrice;
    }

    public String getImageUrl() {
        return coverImage;
    }

    public void setImageUrl(String imageUrl) {
        this.coverImage = imageUrl;
    }

    public String getDescription() {
        return detail;
    }

    public void setDescription(String description) {
        this.detail = description;
    }
}
