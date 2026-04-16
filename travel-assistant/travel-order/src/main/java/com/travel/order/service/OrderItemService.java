package com.travel.order.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.travel.order.entity.OrderItem;

import java.util.List;

/**
 * 订单明细服务
 */
public interface OrderItemService {

    /**
     * 批量添加订单明细
     */
    void batchAddOrderItems(Long orderId, List<OrderItem> orderItems);

    /**
     * 查询订单的所有明细
     */
    List<OrderItem> getOrderItemsByOrderId(Long orderId);

    /**
     * 删除订单明细
     */
    void deleteOrderItem(Long orderItemId);

    /**
     * 分页查询订单明细
     */
    IPage<OrderItem> getOrderItemsByPage(Page<OrderItem> page, Long orderId);
}
