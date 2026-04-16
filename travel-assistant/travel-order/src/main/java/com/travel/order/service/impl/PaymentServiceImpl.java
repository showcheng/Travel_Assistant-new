package com.travel.order.service.impl;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.travel.common.enums.ErrorCode;
import com.travel.common.exception.BusinessException;
import com.travel.order.dto.PaymentCreateRequest;
import com.travel.order.entity.Order;
import com.travel.order.entity.Payment;
import com.travel.order.mapper.OrderMapper;
import com.travel.order.mapper.PaymentMapper;
import com.travel.order.service.OrderService;
import com.travel.order.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 支付服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentMapper paymentMapper;
    private final OrderMapper orderMapper;
    private final OrderService orderService;

    @Override
    @Transactional
    public Payment createPayment(PaymentCreateRequest request, Long userId) {
        log.info("创建支付单: userId={}, orderId={}, amount={}", userId, request.getOrderId(), request.getAmount());

        // 1. 检查订单是否存在
        Order order = orderMapper.selectById(request.getOrderId());
        if (order == null) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        }

        // 2. 检查订单所有权
        if (!order.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        // 3. 检查订单状态
        if (order.getStatus() != 0) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_ERROR);
        }

        // 4. 检查是否已经存在支付单
        QueryWrapper<Payment> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("order_id", request.getOrderId());
        Payment existingPayment = paymentMapper.selectOne(queryWrapper);
        if (existingPayment != null && existingPayment.getStatus() == 2) {
            throw new BusinessException(ErrorCode.PAYMENT_ALREADY_COMPLETED);
        }

        // 5. 创建支付单
        Payment payment = new Payment();
        payment.setPaymentNo(generatePaymentNo());
        payment.setOrderId(request.getOrderId());
        payment.setUserId(userId);
        payment.setAmount(request.getAmount());
        payment.setPayType(request.getPayType());
        payment.setStatus(0); // 0-待支付

        paymentMapper.insert(payment);

        log.info("支付单创建成功: paymentNo={}", payment.getPaymentNo());
        return payment;
    }

    @Override
    @Transactional
    public void paymentCallback(String paymentNo, String transactionId) {
        log.info("收到支付回调: paymentNo={}, transactionId={}", paymentNo, transactionId);

        // 1. 查询支付单
        Payment payment = getPaymentByPaymentNo(paymentNo);
        if (payment == null) {
            log.error("支付单不存在: {}", paymentNo);
            return;
        }

        // 2. 检查支付状态
        if (payment.getStatus() == 2) {
            log.warn("支付单已完成: {}", paymentNo);
            return;
        }

        // 3. 更新支付状态
        payment.setStatus(2); // 2-支付成功
        payment.setTransactionId(transactionId);
        payment.setPayTime(LocalDateTime.now());
        paymentMapper.updateById(payment);

        // 4. 更新订单状态
        orderService.payOrder(payment.getOrderId(), payment.getUserId());

        log.info("支付回调处理成功: paymentNo={}", paymentNo);
    }

    @Override
    public Payment getPaymentByPaymentNo(String paymentNo) {
        QueryWrapper<Payment> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("payment_no", paymentNo);
        return paymentMapper.selectOne(queryWrapper);
    }

    @Override
    public Payment getPaymentByOrderId(Long orderId) {
        QueryWrapper<Payment> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("order_id", orderId);
        return paymentMapper.selectOne(queryWrapper);
    }

    /**
     * 生成支付单号
     */
    private String generatePaymentNo() {
        return "PAY" + System.currentTimeMillis() + IdUtil.getSnowflake(1, 1).nextId();
    }
}
