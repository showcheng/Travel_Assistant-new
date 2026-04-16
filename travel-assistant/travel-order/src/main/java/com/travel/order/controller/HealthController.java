package com.travel.order.controller;

import com.travel.common.response.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 订单服务健康检查
 */
@RestController
@RequestMapping("/api/order")
public class HealthController {

    @GetMapping("/health")
    public Result<Map<String, Object>> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("service", "travel-order");
        health.put("status", "UP");
        health.put("timestamp", LocalDateTime.now());
        return Result.success(health);
    }
}
