package com.travel.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 订单统计数据传输对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderStatisticsDTO {

    /**
     * 总订单数
     */
    private Long totalOrders;

    /**
     * 总订单金额
     */
    private BigDecimal totalAmount;

    /**
     * 各状态订单数量
     */
    private Map<String, Long> statusCount;

    /**
     * 各状态订单金额
     */
    private Map<String, BigDecimal> statusAmount;

    /**
     * 今日订单数
     */
    private Long todayOrders;

    /**
     * 今日订单金额
     */
    private BigDecimal todayAmount;

    /**
     * 本月订单数
     */
    private Long monthOrders;

    /**
     * 本月订单金额
     */
    private BigDecimal monthAmount;

    /**
     * 平均订单金额
     */
    private BigDecimal averageAmount;
}