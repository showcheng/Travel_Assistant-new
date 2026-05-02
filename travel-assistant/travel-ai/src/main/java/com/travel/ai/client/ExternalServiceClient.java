package com.travel.ai.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Client for calling external product and order microservices.
 *
 * Provides resilient HTTP calls with timeout configuration and local fallback
 * responses when external services are unavailable.
 */
@Slf4j
@Service
public class ExternalServiceClient {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(3);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(5);

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${service.product.url:http://localhost:8082}")
    private String productServiceUrl;

    @Value("${service.order.url:http://localhost:8083}")
    private String orderServiceUrl;

    public ExternalServiceClient(RestTemplateBuilder restTemplateBuilder) {
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(CONNECT_TIMEOUT)
                .setReadTimeout(READ_TIMEOUT)
                .build();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Query product recommendations from the product service.
     *
     * @param keyword search keyword (location name, category, etc.)
     * @return formatted product recommendation string, or local fallback on failure
     */
    public String getProducts(String keyword) {
        try {
            String encodedKeyword = URLEncoder.encode(
                    keyword != null ? keyword : "", StandardCharsets.UTF_8);
            String url = productServiceUrl + "/api/products?keyword=" + encodedKeyword;

            log.debug("Calling product service: {}", url);
            String response = restTemplate.getForObject(url, String.class);

            return formatProductResponse(response);
        } catch (Exception e) {
            log.warn("Product service call failed, using local fallback: {}", e.getMessage());
            return getLocalProductFallback(keyword);
        }
    }

    /**
     * Query orders for a specific user from the order service.
     *
     * @param userId the user ID to query orders for
     * @return formatted order information string, or local fallback on failure
     */
    public String getOrders(Long userId) {
        try {
            Long effectiveUserId = userId != null ? userId : 1L;
            String url = orderServiceUrl + "/api/orders?userId=" + effectiveUserId;

            log.debug("Calling order service: {}", url);
            String response = restTemplate.getForObject(url, String.class);

            return formatOrderResponse(response);
        } catch (Exception e) {
            log.warn("Order service call failed, using local fallback: {}", e.getMessage());
            return getLocalOrderFallback(userId);
        }
    }

    /**
     * Parse product JSON response and format as a readable Chinese text.
     * Handles null, empty, and malformed JSON defensively.
     */
    String formatProductResponse(String json) {
        if (json == null || json.isBlank()) {
            return getLocalProductFallback("");
        }

        try {
            List<Map<String, Object>> products = objectMapper.readValue(
                    json, new TypeReference<List<Map<String, Object>>>() {});

            if (products.isEmpty()) {
                return "暂未找到相关产品，请尝试其他关键词搜索。";
            }

            StringBuilder sb = new StringBuilder("为您推荐以下产品：\n");
            for (int i = 0; i < products.size(); i++) {
                Map<String, Object> product = products.get(i);
                String name = String.valueOf(product.getOrDefault("name", "未知产品"));
                Object price = product.get("price");
                sb.append(i + 1).append(". ").append(name);
                if (price != null) {
                    sb.append(" - ¥").append(price);
                }
                sb.append("\n");
            }
            return sb.toString().trim();
        } catch (Exception e) {
            log.warn("Failed to parse product response: {}", e.getMessage());
            return getLocalProductFallback("");
        }
    }

    /**
     * Parse order JSON response and format as a readable Chinese text.
     * Handles null, empty, and malformed JSON defensively.
     */
    String formatOrderResponse(String json) {
        if (json == null || json.isBlank()) {
            return getLocalOrderFallback(1L);
        }

        try {
            List<Map<String, Object>> orders = objectMapper.readValue(
                    json, new TypeReference<List<Map<String, Object>>>() {});

            if (orders.isEmpty()) {
                return "您目前没有订单记录。";
            }

            StringBuilder sb = new StringBuilder("您共有 ")
                    .append(orders.size())
                    .append(" 个订单：\n");

            for (Map<String, Object> order : orders) {
                String productName = String.valueOf(order.getOrDefault("productName", "未知产品"));
                String status = formatOrderStatus(String.valueOf(order.getOrDefault("status", "UNKNOWN")));
                Object amount = order.get("amount");

                sb.append("- ").append(productName);
                if (amount != null) {
                    sb.append(" ¥").append(amount);
                }
                sb.append(" [").append(status).append("]\n");
            }
            return sb.toString().trim();
        } catch (Exception e) {
            log.warn("Failed to parse order response: {}", e.getMessage());
            return getLocalOrderFallback(1L);
        }
    }

    /**
     * Local fallback when product service is unavailable.
     */
    private String getLocalProductFallback(String keyword) {
        return "为您推荐以下热门产品：故宫博物院成人票、长城一日游、颐和园门票。\n"
                + "（注：实时产品服务暂不可用，以上为推荐列表）";
    }

    /**
     * Local fallback when order service is unavailable.
     */
    private String getLocalOrderFallback(Long userId) {
        return "订单服务暂不可用，请稍后再试。您也可以在【我的订单】页面查看订单信息。";
    }

    /**
     * Convert order status enum to user-friendly Chinese text.
     */
    private String formatOrderStatus(String status) {
        if (status == null) {
            return "未知";
        }
        return switch (status) {
            case "COMPLETED" -> "已完成";
            case "PENDING_PAYMENT" -> "待支付";
            case "PENDING_USE" -> "待使用";
            case "CANCELLED" -> "已取消";
            case "REFUNDED" -> "已退款";
            default -> status;
        };
    }
}
