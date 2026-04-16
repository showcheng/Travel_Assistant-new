package com.travel.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.travel.common.enums.ErrorCode;
import com.travel.common.exception.BusinessException;
import com.travel.order.entity.OrderItem;
import com.travel.order.mapper.OrderItemMapper;
import com.travel.order.service.OrderItemService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * 订单明细服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderItemServiceImpl implements OrderItemService {

    private final OrderItemMapper orderItemMapper;

    @Override
    @Transactional
    public void batchAddOrderItems(Long orderId, List<OrderItem> orderItems) {
        log.info("批量添加订单明细: orderId={}, itemsCount={}", orderId, orderItems.size());

        for (OrderItem item : orderItems) {
            item.setOrderId(orderId);
            // 计算小计金额
            item.setTotalPrice(item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
            orderItemMapper.insert(item);
        }

        log.info("订单明细添加成功: orderId={}, count={}", orderId, orderItems.size());
    }

    @Override
    public List<OrderItem> getOrderItemsByOrderId(Long orderId) {
        log.info("查询订单明细: orderId={}", orderId);

        QueryWrapper<OrderItem> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("order_id", orderId);
        queryWrapper.orderByDesc("create_time");

        List<OrderItem> orderItems = orderItemMapper.selectList(queryWrapper);
        log.info("查询到 {} 条订单明细", orderItems.size());

        return orderItems;
    }

    @Override
    @Transactional
    public void deleteOrderItem(Long orderItemId) {
        log.info("删除订单明细: orderItemId={}", orderItemId);

        OrderItem orderItem = orderItemMapper.selectById(orderItemId);
        if (orderItem == null) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        }

        orderItemMapper.deleteById(orderItemId);

        log.info("订单明细删除成功: orderItemId={}", orderItemId);
    }

    @Override
    public IPage<OrderItem> getOrderItemsByPage(Page<OrderItem> page, Long orderId) {
        log.info("分页查询订单明细: orderId={}, page={}, size={}", orderId, page.getCurrent(), page.getSize());

        QueryWrapper<OrderItem> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("order_id", orderId);
        queryWrapper.orderByDesc("create_time");

        IPage<OrderItem> resultPage = orderItemMapper.selectPage(page, queryWrapper);
        log.info("查询到 {} 条订单明细", resultPage.getTotal());

        return resultPage;
    }
}
