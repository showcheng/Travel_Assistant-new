package com.travel.order.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.travel.common.response.Result;
import com.travel.common.utils.ExcelUtil;
import com.travel.common.utils.UserContext;
import com.travel.order.dto.OrderCreateRequest;
import com.travel.order.dto.OrderStatisticsDTO;
import com.travel.order.entity.Order;
import com.travel.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 订单控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Tag(name = "订单管理", description = "订单的增删改查接口")
public class OrderController {

    private final OrderService orderService;

    /**
     * 获取当前登录用户ID
     * 从UserContext中获取用户ID
     */
    private Long getCurrentUserId() {
        return UserContext.getCurrentUserId();
    }

    @PostMapping
    @Operation(summary = "创建订单", description = "创建新的订单")
    public Result<Long> createOrder(@Valid @RequestBody OrderCreateRequest request) {
        // 从SecurityContext中获取用户ID
        Long userId = getCurrentUserId();
        log.info("创建订单请求: userId={}, productId={}", userId, request.getProductId());
        Long orderId = orderService.createOrder(request, userId);
        return Result.success(orderId);
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "取消订单", description = "取消待支付的订单")
    public Result<Void> cancelOrder(@PathVariable Long id) {
        Long userId = getCurrentUserId();
        log.info("取消订单请求: orderId={}, userId={}", id, userId);
        orderService.cancelOrder(id, userId);
        return Result.success();
    }

    @PostMapping("/{id}/pay")
    @Operation(summary = "支付订单", description = "支付订单")
    public Result<Void> payOrder(@PathVariable Long id) {
        Long userId = getCurrentUserId();
        log.info("支付订单请求: orderId={}, userId={}", id, userId);
        orderService.payOrder(id, userId);
        return Result.success();
    }

    @PostMapping("/{id}/complete")
    @Operation(summary = "完成订单", description = "完成已支付的订单")
    public Result<Void> completeOrder(@PathVariable Long id) {
        Long userId = getCurrentUserId();
        log.info("完成订单请求: orderId={}, userId={}", id, userId);
        orderService.completeOrder(id, userId);
        return Result.success();
    }

    @PostMapping("/{id}/refund")
    @Operation(summary = "申请退款", description = "申请订单退款")
    public Result<Void> refundOrder(@PathVariable Long id) {
        Long userId = getCurrentUserId();
        log.info("申请退款请求: orderId={}, userId={}", id, userId);
        orderService.refundOrder(id, userId);
        return Result.success();
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取订单详情", description = "根据ID获取订单详情")
    public Result<Order> getOrderById(@PathVariable Long id) {
        Long userId = getCurrentUserId();
        log.info("查询订单请求: id={}, userId={}", id, userId);
        Order order = orderService.getOrderById(id, userId);
        return Result.success(order);
    }

    @GetMapping
    @Operation(summary = "分页查询用户订单", description = "分页查询当前用户的订单列表")
    public Result<Page<Order>> getUserOrders(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String status) {
        Long userId = getCurrentUserId();
        log.info("查询用户订单请求: userId={}, page={}, size={}, status={}", userId, page, size, status);
        Page<Order> orders = orderService.getUserOrders(userId, page, size, status);
        return Result.success(orders);
    }

    @GetMapping("/search")
    @Operation(summary = "搜索订单", description = "根据订单号或商品名称搜索订单")
    public Result<Page<Order>> searchOrders(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        Long userId = getCurrentUserId();
        log.info("搜索订单请求: userId={}, keyword={}, page={}, size={}", userId, keyword, page, size);
        Page<Order> orders = orderService.searchOrders(userId, keyword, page, size);
        return Result.success(orders);
    }

    @GetMapping("/statistics")
    @Operation(summary = "获取订单统计", description = "获取用户的订单统计数据")
    public Result<OrderStatisticsDTO> getOrderStatistics() {
        Long userId = getCurrentUserId();
        log.info("获取订单统计请求: userId={}", userId);
        OrderStatisticsDTO statistics = orderService.getOrderStatistics(userId);
        return Result.success(statistics);
    }

    @GetMapping("/export")
    @Operation(summary = "导出订单数据", description = "导出用户的订单数据为Excel文件")
    public void exportOrders(HttpServletResponse response) {
        Long userId = getCurrentUserId();
        log.info("导出订单数据请求: userId={}", userId);

        try {
            // 查询用户所有订单
            List<Order> orders = orderService.getUserAllOrders(userId);

            // 生成文件名
            String fileName = "orders_" + userId + "_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".xlsx";
            String sheetName = "订单数据";

            // 导出Excel
            ExcelUtil.exportExcel(fileName, sheetName, orders, response);

            log.info("订单数据导出成功: userId={}, count={}", userId, orders.size());
        } catch (Exception e) {
            log.error("导出订单数据失败: userId={}, error={}", userId, e.getMessage(), e);
            throw new RuntimeException("导出订单数据失败: " + e.getMessage());
        }
    }

}
