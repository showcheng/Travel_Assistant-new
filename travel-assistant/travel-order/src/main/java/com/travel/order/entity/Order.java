package com.travel.order.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单实体
 */
@Data
@TableName("orders")
public class Order {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 订单号
     */
    private String orderNo;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 订单总金额
     */
    private BigDecimal totalAmount;

    /**
     * 实付金额
     */
    private BigDecimal payAmount;

    /**
     * 订单状态：0-待支付，1-已支付，2-已取消，3-已退款
     */
    private Integer status;

    /**
     * 支付方式：1-支付宝，2-微信
     */
    private Integer payType;

    /**
     * 支付时间
     */
    private LocalDateTime payTime;

    /**
     * 逻辑删除：0-未删除，1-已删除
     */
    private Integer deleted;

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
    public BigDecimal getTotalPrice() {
        return totalAmount;
    }

    public void setTotalPrice(BigDecimal totalPrice) {
        this.totalAmount = totalPrice;
    }

    public LocalDateTime getPaidAt() {
        return payTime;
    }

    public void setPaidAt(LocalDateTime paidAt) {
        this.payTime = paidAt;
    }

    public LocalDateTime getCreatedAt() {
        return createTime;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createTime = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updateTime;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updateTime = updatedAt;
    }
}
