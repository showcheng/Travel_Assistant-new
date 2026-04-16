package com.travel.ai.controller;

import com.travel.common.response.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * AI服务健康检查控制器
 */
@RestController
@RequestMapping("/api/ai/health")
public class AIHealthController {

    /**
     * 健康检查接口
     */
    @GetMapping
    public Result<Map<String, Object>> health() {
        Map<String, Object> healthInfo = new HashMap<>();
        healthInfo.put("service", "travel-ai");
        healthInfo.put("status", "UP");
        healthInfo.put("timestamp", LocalDateTime.now());
        healthInfo.put("version", "1.0.0");

        return Result.success(healthInfo);
    }

    /**
     * 就绪检查
     */
    @GetMapping("/ready")
    public Result<Map<String, Object>> ready() {
        Map<String, Object> readyInfo = new HashMap<>();
        readyInfo.put("ready", true);
        readyInfo.put("timestamp", LocalDateTime.now());

        return Result.success(readyInfo);
    }
}
