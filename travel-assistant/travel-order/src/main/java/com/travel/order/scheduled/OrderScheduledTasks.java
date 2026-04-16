package com.travel.order.scheduled;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.travel.order.entity.Order;
import com.travel.order.enums.OrderStatus;
import com.travel.order.mapper.OrderMapper;
import com.travel.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单定时任务
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderScheduledTasks {

    private final OrderMapper orderMapper;
    private final OrderService orderService;

    /**
     * 自动取消超时未支付订单
     * 每1分钟执行一次
     */
    @Scheduled(fixedRate = 60000) // 每1分钟执行一次
    public void autoCancelExpiredOrders() {
        try {
            log.info("开始执行超时订单自动取消任务");

            // 1. 查询所有待支付订单
            QueryWrapper<Order> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("status", OrderStatus.PENDING_PAYMENT.getCode());
            List<Order> pendingOrders = orderMapper.selectList(queryWrapper);

            if (pendingOrders.isEmpty()) {
                log.info("没有待支付订单需要处理");
                return;
            }

            log.info("找到 {} 个待支付订单", pendingOrders.size());

            // 2. 检查订单是否超时（30分钟）
            LocalDateTime timeout = LocalDateTime.now().minusMinutes(30);
            int cancelledCount = 0;

            for (Order order : pendingOrders) {
                if (order.getCreateTime() != null && order.getCreateTime().isBefore(timeout)) {
                    try {
                        log.info("订单超时自动取消: orderId={}, orderNo={}, createTime={}",
                            order.getId(), order.getOrderNo(), order.getCreateTime());

                        // 调用取消订单服务
                        orderService.cancelOrder(order.getId(), order.getUserId());
                        cancelledCount++;

                    } catch (Exception e) {
                        log.error("自动取消订单失败: orderId={}, error={}",
                            order.getId(), e.getMessage(), e);
                    }
                }
            }

            if (cancelledCount > 0) {
                log.info("成功自动取消 {} 个超时订单", cancelledCount);
            } else {
                log.info("没有订单需要自动取消");
            }

        } catch (Exception e) {
            log.error("执行超时订单自动取消任务失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 订单统计日志任务
     * 每小时执行一次
     */
    @Scheduled(fixedRate = 3600000) // 每小时执行一次
    public void logOrderStatistics() {
        try {
            log.info("开始执行订单统计任务");

            QueryWrapper<Order> queryWrapper = new QueryWrapper<>();
            List<Order> allOrders = orderMapper.selectList(queryWrapper);

            long pendingCount = allOrders.stream()
                .filter(order -> order.getStatus().equals(OrderStatus.PENDING_PAYMENT.getCode()))
                .count();

            long paidCount = allOrders.stream()
                .filter(order -> order.getStatus().equals(OrderStatus.PAID.getCode()))
                .count();

            long completedCount = allOrders.stream()
                .filter(order -> order.getStatus().equals(OrderStatus.COMPLETED.getCode()))
                .count();

            long cancelledCount = allOrders.stream()
                .filter(order -> order.getStatus().equals(OrderStatus.CANCELLED.getCode()))
                .count();

            long refundedCount = allOrders.stream()
                .filter(order -> order.getStatus().equals(OrderStatus.REFUNDED.getCode()))
                .count();

            log.info("订单统计 - 总订单数: {}, 待支付: {}, 已支付: {}, 已完成: {}, 已取消: {}, 已退款: {}",
                allOrders.size(), pendingCount, paidCount, completedCount, cancelledCount, refundedCount);

        } catch (Exception e) {
            log.error("执行订单统计任务失败: {}", e.getMessage(), e);
        }
    }
}