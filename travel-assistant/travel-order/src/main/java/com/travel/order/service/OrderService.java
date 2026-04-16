package com.travel.order.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.travel.order.dto.OrderCreateRequest;
import com.travel.order.entity.Order;

import java.util.List;

/**
 * 订单服务接口
 */
public interface OrderService {

    /**
     * 创建订单
     */
    Long createOrder(OrderCreateRequest request, Long userId);

    /**
     * 取消订单
     */
    void cancelOrder(Long orderId, Long userId);

    /**
     * 支付订单
     */
    void payOrder(Long orderId, Long userId);

    /**
     * 根据ID获取订单
     */
    Order getOrderById(Long id, Long userId);

    /**
     * 分页查询用户订单
     */
    Page<Order> getUserOrders(Long userId, Integer page, Integer size, String status);

    /**
     * 搜索订单
     */
    Page<Order> searchOrders(Long userId, String keyword, Integer page, Integer size);

    /**
     * 订单状态更新
     */
    void updateOrderStatus(Long orderId, String status);

    /**
     * 完成订单
     */
    void completeOrder(Long orderId, Long userId);

    /**
     * 申请退款
     */
    void refundOrder(Long orderId, Long userId);

    /**
     * 获取订单统计数据
     */
    com.travel.order.dto.OrderStatisticsDTO getOrderStatistics(Long userId);

    /**
     * 获取用户所有订单（用于导出）
     */
    List<Order> getUserAllOrders(Long userId);
}
