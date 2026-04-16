package com.travel.order.service;

import com.travel.order.dto.PaymentCreateRequest;
import com.travel.order.entity.Payment;

/**
 * 支付服务
 */
public interface PaymentService {

    /**
     * 创建支付单
     */
    Payment createPayment(PaymentCreateRequest request, Long userId);

    /**
     * 模拟支付回调
     */
    void paymentCallback(String paymentNo, String transactionId);

    /**
     * 查询支付状态
     */
    Payment getPaymentByPaymentNo(String paymentNo);

    /**
     * 根据订单ID查询支付信息
     */
    Payment getPaymentByOrderId(Long orderId);
}
