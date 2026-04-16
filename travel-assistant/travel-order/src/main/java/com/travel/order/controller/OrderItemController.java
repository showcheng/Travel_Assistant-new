package com.travel.order.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.travel.common.response.Result;
import com.travel.common.utils.UserContext;
import com.travel.order.entity.OrderItem;
import com.travel.order.service.OrderItemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 订单明细控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/order-items")
@RequiredArgsConstructor
@Tag(name = "订单明细管理", description = "订单明细的增删改查接口")
public class OrderItemController {

    private final OrderItemService orderItemService;

    @GetMapping("/order/{orderId}")
    @Operation(summary = "查询订单的所有明细", description = "根据订单ID查询所有明细")
    public Result<java.util.List<OrderItem>> getOrderItemsByOrderId(@PathVariable Long orderId) {
        log.info("查询订单明细: orderId={}", orderId);
        java.util.List<OrderItem> orderItems = orderItemService.getOrderItemsByOrderId(orderId);
        return Result.success(orderItems);
    }

    @GetMapping("/order/{orderId}/page")
    @Operation(summary = "分页查询订单明细", description = "分页查询指定订单的明细")
    public Result<IPage<OrderItem>> getOrderItemsByPage(
            @PathVariable Long orderId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        log.info("分页查询订单明细: orderId={}, page={}, size={}", orderId, page, size);
        
        Page<OrderItem> pageParam = new Page<>(page, size);
        IPage<OrderItem> resultPage = orderItemService.getOrderItemsByPage(pageParam, orderId);
        
        return Result.success(resultPage);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除订单明细", description = "删除指定的订单明细")
    public Result<Void> deleteOrderItem(@PathVariable Long id) {
        log.info("删除订单明细: id={}", id);
        Long userId = getCurrentUserId();
        orderItemService.deleteOrderItem(id);
        return Result.success();
    }

    /**
     * 获取当前用户ID
     * 从UserContext中获取用户ID
     */
    private Long getCurrentUserId() {
        return UserContext.getCurrentUserId();
    }
}
