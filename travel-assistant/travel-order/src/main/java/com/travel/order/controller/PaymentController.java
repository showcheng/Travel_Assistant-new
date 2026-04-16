package com.travel.order.controller;

import com.travel.common.response.Result;
import com.travel.order.dto.PaymentCreateRequest;
import com.travel.order.entity.Payment;
import com.travel.order.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

/**
 * 支付控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Tag(name = "支付管理", description = "支付相关接口")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    @Operation(summary = "创建支付单", description = "创建新的支付单")
    public Result<Payment> createPayment(@Valid @RequestBody PaymentCreateRequest request) {
        Long userId = getCurrentUserId();
        log.info("创建支付单请求: userId={}, orderId={}", userId, request.getOrderId());
        Payment payment = paymentService.createPayment(request, userId);
        return Result.success(payment);
    }

    @PostMapping("/{paymentNo}/callback")
    @Operation(summary = "模拟支付回调", description = "模拟第三方支付回调接口")
    public Result<Void> paymentCallback(
            @PathVariable String paymentNo,
            @RequestParam(required = false) String transactionId
    ) {
        log.info("支付回调请求: paymentNo={}", paymentNo);
        // 如果没有提供transactionId，生成一个模拟的
        if (transactionId == null || transactionId.isEmpty()) {
            transactionId = "TXN" + System.currentTimeMillis();
        }
        paymentService.paymentCallback(paymentNo, transactionId);
        return Result.success();
    }

    @GetMapping("/{paymentNo}")
    @Operation(summary = "查询支付状态", description = "根据支付单号查询支付状态")
    public Result<Payment> getPaymentByPaymentNo(@PathVariable String paymentNo) {
        log.info("查询支付状态: paymentNo={}", paymentNo);
        Payment payment = paymentService.getPaymentByPaymentNo(paymentNo);
        if (payment == null) {
            return Result.error(404, "支付记录不存在");
        }
        return Result.success(payment);
    }

    @GetMapping("/order/{orderId}")
    @Operation(summary = "查询订单支付信息", description = "根据订单ID查询支付信息")
    public Result<Payment> getPaymentByOrderId(@PathVariable Long orderId) {
        Long userId = getCurrentUserId();
        log.info("查询订单支付信息: userId={}, orderId={}", userId, orderId);
        Payment payment = paymentService.getPaymentByOrderId(orderId);
        if (payment == null) {
            return Result.error(404, "支付记录不存在");
        }
        return Result.success(payment);
    }

    /**
     * 获取当前登录用户ID
     */
    private Long getCurrentUserId() {
        org.springframework.web.context.request.RequestAttributes requestAttributes =
            org.springframework.web.context.request.RequestContextHolder.getRequestAttributes();
        if (requestAttributes instanceof org.springframework.web.context.request.ServletRequestAttributes) {
            jakarta.servlet.http.HttpServletRequest request =
                ((org.springframework.web.context.request.ServletRequestAttributes) requestAttributes).getRequest();
            String userIdHeader = request.getHeader("X-User-Id");
            if (userIdHeader != null) {
                try {
                    return Long.parseLong(userIdHeader);
                } catch (NumberFormatException e) {
                    log.warn("Invalid X-User-Id header: {}", userIdHeader);
                }
            }
        }

        // 备用方案：从SecurityContext获取
        Object principal = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof Long) {
            return (Long) principal;
        }
        throw new RuntimeException("用户未登录");
    }
}
