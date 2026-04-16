package com.travel.order.service.impl;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.travel.common.enums.ErrorCode;
import com.travel.common.exception.BusinessException;
import com.travel.order.dto.OrderCreateRequest;
import com.travel.order.dto.OrderItemCreateRequest;
import com.travel.order.dto.OrderStatisticsDTO;
import com.travel.order.entity.Order;
import com.travel.order.entity.OrderItem;
import com.travel.order.enums.OrderStatus;
import com.travel.order.feign.ProductClient;
import com.travel.order.feign.dto.ProductDTO;
import com.travel.order.mapper.OrderMapper;
import com.travel.order.service.OrderItemService;
import com.travel.order.service.OrderService;
import com.travel.order.websocket.OrderWebSocketServer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 订单服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderMapper orderMapper;
    private final OrderItemService orderItemService;
    private final ProductClient productClient;

    @Override
    @Transactional
    public Long createOrder(OrderCreateRequest request, Long userId) {
        log.info("创建订单: userId={}, itemsCount={}", userId, request.getItems().size());

        // 1. 验证订单明细
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR);
        }

        // 2. 验证商品并扣减库存，同时获取实际价格
        List<OrderItem> orderItems = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (OrderItemCreateRequest itemRequest : request.getItems()) {
            try {
                // 调用商品服务获取商品信息
                ProductClient.StockDecreaseRequest stockRequest =
                    new ProductClient.StockDecreaseRequest(itemRequest.getQuantity());
                productClient.decreaseStock(itemRequest.getProductId(), stockRequest);

                // 获取商品信息
                var productResult = productClient.getProductById(itemRequest.getProductId());
                if (productResult == null || productResult.getData() == null) {
                    throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
                }

                ProductDTO product = productResult.getData();

                // 使用实际价格
                BigDecimal actualPrice = product.getCurrentPrice();
                BigDecimal itemTotal = actualPrice.multiply(BigDecimal.valueOf(itemRequest.getQuantity()));

                // 创建订单明细
                OrderItem item = new OrderItem();
                item.setProductId(product.getId());
                item.setProductName(product.getProductName());
                item.setProductImage(product.getProductImage());
                item.setQuantity(itemRequest.getQuantity());
                item.setPrice(actualPrice);
                item.setTotalPrice(itemTotal);

                orderItems.add(item);
                totalAmount = totalAmount.add(itemTotal);

                log.info("商品验证成功: productId={}, price={}, quantity={}, total={}",
                    product.getId(), actualPrice, itemRequest.getQuantity(), itemTotal);

            } catch (Exception e) {
                log.error("商品验证或库存扣减失败: productId={}, error={}",
                    itemRequest.getProductId(), e.getMessage());
                throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
            }
        }

        // 3. 创建订单
        Order order = new Order();
        order.setOrderNo(generateOrderNo());
        order.setUserId(userId);
        order.setTotalAmount(totalAmount);
        order.setPayAmount(totalAmount);
        order.setStatus(OrderStatus.PENDING_PAYMENT.getCode());

        orderMapper.insert(order);

        // 4. 设置订单ID并批量添加订单明细
        for (OrderItem item : orderItems) {
            item.setOrderId(order.getId());
        }
        orderItemService.batchAddOrderItems(order.getId(), orderItems);

        log.info("订单创建成功: orderId={}, orderNo={}, itemsCount={}, totalAmount={}",
            order.getId(), order.getOrderNo(), orderItems.size(), totalAmount);
        return order.getId();
    }

    @Override
    @Transactional
    public void cancelOrder(Long orderId, Long userId) {
        log.info("取消订单: orderId={}, userId={}", orderId, userId);

        // 1. 检查订单是否存在
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        }

        // 2. 检查订单所有权
        if (!order.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        // 3. 检查订单状态
        OrderStatus currentStatus = OrderStatus.getByCode(order.getStatus());
        if (currentStatus == null) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_ERROR);
        }

        // 4. 检查是否可以取消
        if (!currentStatus.canTransitionTo(OrderStatus.CANCELLED)) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_ERROR);
        }

        // 5. 恢复库存
        try {
            List<OrderItem> orderItems = orderItemService.getOrderItemsByOrderId(orderId);
            for (OrderItem item : orderItems) {
                // 这里需要创建一个对应的增加库存请求类
                // 暂时跳过，可以后续实现
                log.info("恢复库存: productId={}, quantity={}", item.getProductId(), item.getQuantity());
            }
        } catch (Exception e) {
            log.error("恢复库存失败: orderId={}, error={}", orderId, e.getMessage());
            // 这里可以记录日志，但不影响订单取消流程
        }

        // 6. 更新订单状态
        order.setStatus(OrderStatus.CANCELLED.getCode());
        orderMapper.updateById(order);

        // 7. 发送WebSocket通知
        OrderWebSocketServer.sendOrderNotification(
            userId,
            order.getId(),
            order.getOrderNo(),
            OrderStatus.CANCELLED.getCode(),
            OrderStatus.CANCELLED.getDescription()
        );

        log.info("订单取消成功: orderId={}", orderId);
    }

    @Override
    @Transactional
    public void payOrder(Long orderId, Long userId) {
        log.info("支付订单: orderId={}, userId={}", orderId, userId);

        // 1. 检查订单是否存在
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        }

        // 2. 检查订单所有权
        if (!order.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        // 3. 检查订单状态
        OrderStatus currentStatus = OrderStatus.getByCode(order.getStatus());
        if (currentStatus == null) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_ERROR);
        }

        // 4. 检查是否可以支付
        if (!currentStatus.canTransitionTo(OrderStatus.PAID)) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_ERROR);
        }

        // 5. TODO: 调用支付服务
        // PaymentResult paymentResult = paymentClient.pay(order);

        // 6. 更新订单状态
        order.setStatus(OrderStatus.PAID.getCode());
        order.setPayTime(LocalDateTime.now());
        orderMapper.updateById(order);

        // 7. 发送WebSocket通知
        OrderWebSocketServer.sendOrderNotification(
            userId,
            order.getId(),
            order.getOrderNo(),
            OrderStatus.PAID.getCode(),
            OrderStatus.PAID.getDescription()
        );

        log.info("订单支付成功: orderId={}", orderId);
    }

    @Override
    public Order getOrderById(Long id, Long userId) {
        log.info("查询订单: id={}, userId={}", id, userId);

        Order order = orderMapper.selectById(id);
        if (order == null) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        }

        // 检查订单所有权
        if (!order.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        return order;
    }

    @Override
    public Page<Order> getUserOrders(Long userId, Integer page, Integer size, String status) {
        log.info("查询用户订单: userId={}, page={}, size={}, status={}", userId, page, size, status);

        // 1. 构建查询条件
        QueryWrapper<Order> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId);

        if (status != null && !status.isEmpty()) {
            try {
                queryWrapper.eq("status", Integer.parseInt(status));
            } catch (NumberFormatException e) {
                log.warn("Invalid status format: {}", status);
            }
        }

        queryWrapper.orderByDesc("create_time");

        // 2. 分页查询
        Page<Order> pageParam = new Page<>(page, size);
        Page<Order> resultPage = orderMapper.selectPage(pageParam, queryWrapper);

        log.info("查询到 {} 个订单", resultPage.getTotal());
        return resultPage;
    }

    @Override
    public Page<Order> searchOrders(Long userId, String keyword, Integer page, Integer size) {
        log.info("搜索订单: userId={}, keyword={}, page={}, size={}", userId, keyword, page, size);

        // 1. 构建查询条件
        QueryWrapper<Order> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId);

        // 搜索订单号或商品名称（通过关联查询）
        if (keyword != null && !keyword.trim().isEmpty()) {
            // 搜索订单号
            queryWrapper.and(wrapper -> wrapper
                .like("order_no", keyword)
                .or()
                .inSql("id",
                    "SELECT order_id FROM order_item WHERE product_name LIKE '%" + keyword + "%'"
                )
            );
        }

        queryWrapper.orderByDesc("create_time");

        // 2. 分页查询
        Page<Order> pageParam = new Page<>(page, size);
        Page<Order> resultPage = orderMapper.selectPage(pageParam, queryWrapper);

        log.info("搜索到 {} 个订单", resultPage.getTotal());
        return resultPage;
    }

    @Override
    @Transactional
    public void updateOrderStatus(Long orderId, String status) {
        log.info("更新订单状态: orderId={}, status={}", orderId, status);

        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        }

        // 获取当前状态
        OrderStatus currentStatus = OrderStatus.getByCode(order.getStatus());
        if (currentStatus == null) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_ERROR);
        }

        // 根据状态字符串转换为枚举
        OrderStatus targetStatus;
        try {
            targetStatus = OrderStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_ERROR);
        }

        // 检查是否可以转换
        if (!currentStatus.canTransitionTo(targetStatus)) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_ERROR);
        }

        order.setStatus(targetStatus.getCode());

        // 根据状态设置相应的时间戳
        if (targetStatus == OrderStatus.PAID) {
            order.setPayTime(LocalDateTime.now());
        }

        orderMapper.updateById(order);

        log.info("订单状态更新成功: orderId={}, status={}", orderId, targetStatus);
    }

    @Override
    @Transactional
    public void completeOrder(Long orderId, Long userId) {
        log.info("完成订单: orderId={}, userId={}", orderId, userId);

        // 1. 检查订单是否存在
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        }

        // 2. 检查订单所有权
        if (!order.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        // 3. 检查订单状态
        OrderStatus currentStatus = OrderStatus.getByCode(order.getStatus());
        if (currentStatus == null) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_ERROR);
        }

        // 4. 检查是否可以完成
        if (!currentStatus.canTransitionTo(OrderStatus.COMPLETED)) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_ERROR);
        }

        // 5. 更新订单状态
        order.setStatus(OrderStatus.COMPLETED.getCode());
        orderMapper.updateById(order);

        log.info("订单完成: orderId={}", orderId);
    }

    @Override
    @Transactional
    public void refundOrder(Long orderId, Long userId) {
        log.info("申请退款: orderId={}, userId={}", orderId, userId);

        // 1. 检查订单是否存在
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        }

        // 2. 检查订单所有权
        if (!order.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        // 3. 检查订单状态
        OrderStatus currentStatus = OrderStatus.getByCode(order.getStatus());
        if (currentStatus == null) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_ERROR);
        }

        // 4. 检查是否可以退款（只有已支付状态可以退款）
        if (!currentStatus.canTransitionTo(OrderStatus.REFUNDED)) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_ERROR);
        }

        // 5. 恢复库存
        try {
            List<OrderItem> orderItems = orderItemService.getOrderItemsByOrderId(orderId);
            for (OrderItem item : orderItems) {
                // 调用商品服务增加库存
                ProductClient.StockIncreaseRequest stockRequest =
                    new ProductClient.StockIncreaseRequest(item.getQuantity());
                productClient.increaseStock(item.getProductId(), stockRequest);
                log.info("恢复库存成功: productId={}, quantity={}", item.getProductId(), item.getQuantity());
            }
        } catch (Exception e) {
            log.error("恢复库存失败: orderId={}, error={}", orderId, e.getMessage());
            // 库存恢复失败不影响退款流程，记录日志即可
        }

        // 6. 更新订单状态为已退款
        order.setStatus(OrderStatus.REFUNDED.getCode());
        orderMapper.updateById(order);

        // 7. 发送WebSocket通知
        OrderWebSocketServer.sendOrderNotification(
            userId,
            order.getId(),
            order.getOrderNo(),
            OrderStatus.REFUNDED.getCode(),
            OrderStatus.REFUNDED.getDescription()
        );

        log.info("订单退款成功: orderId={}", orderId);
    }

    /**
     * 生成订单号
     */
    private String generateOrderNo() {
        return "ORD" + System.currentTimeMillis() + IdUtil.getSnowflake(1, 1).nextId();
    }

    @Override
    public OrderStatisticsDTO getOrderStatistics(Long userId) {
        log.info("获取订单统计: userId={}", userId);

        // 1. 查询所有订单
        QueryWrapper<Order> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId);
        List<Order> orders = orderMapper.selectList(queryWrapper);

        // 2. 基础统计
        long totalOrders = orders.size();
        BigDecimal totalAmount = orders.stream()
            .map(Order::getPayAmount)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 3. 按状态统计
        Map<String, Long> statusCount = new HashMap<>();
        Map<String, BigDecimal> statusAmount = new HashMap<>();

        for (OrderStatus status : OrderStatus.values()) {
            long count = orders.stream()
                .filter(order -> status.getCode().equals(order.getStatus()))
                .count();

            BigDecimal amount = orders.stream()
                .filter(order -> status.getCode().equals(order.getStatus()))
                .map(Order::getPayAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

            statusCount.put(status.getDescription(), count);
            statusAmount.put(status.getDescription(), amount);
        }

        // 4. 今日统计
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        List<Order> todayOrders = orders.stream()
            .filter(order -> order.getCreateTime() != null && !order.getCreateTime().isBefore(todayStart))
            .collect(Collectors.toList());

        long todayCount = todayOrders.size();
        BigDecimal todayAmount = todayOrders.stream()
            .map(Order::getPayAmount)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 5. 本月统计
        LocalDateTime monthStart = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        List<Order> monthOrdersList = orders.stream()
            .filter(order -> order.getCreateTime() != null && !order.getCreateTime().isBefore(monthStart))
            .collect(Collectors.toList());

        long monthCount = monthOrdersList.size();
        BigDecimal monthAmount = monthOrdersList.stream()
            .map(Order::getPayAmount)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 6. 平均订单金额
        BigDecimal averageAmount = totalOrders > 0 ?
            totalAmount.divide(BigDecimal.valueOf(totalOrders), 2, RoundingMode.HALF_UP) :
            BigDecimal.ZERO;

        return OrderStatisticsDTO.builder()
            .totalOrders(totalOrders)
            .totalAmount(totalAmount)
            .statusCount(statusCount)
            .statusAmount(statusAmount)
            .todayOrders(todayCount)
            .todayAmount(todayAmount)
            .monthOrders(monthCount)
            .monthAmount(monthAmount)
            .averageAmount(averageAmount)
            .build();
    }

    @Override
    public List<Order> getUserAllOrders(Long userId) {
        log.info("获取用户所有订单: userId={}", userId);

        QueryWrapper<Order> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId);
        queryWrapper.orderByDesc("create_time");

        List<Order> orders = orderMapper.selectList(queryWrapper);

        log.info("查询到 {} 个订单", orders.size());
        return orders;
    }
}
