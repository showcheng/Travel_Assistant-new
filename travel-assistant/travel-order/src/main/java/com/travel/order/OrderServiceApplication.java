package com.travel.order;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 订单服务启动类
 */
@SpringBootApplication(scanBasePackages = "com.travel")
@MapperScan("com.travel.order.mapper")
@EnableFeignClients(basePackages = "com.travel.order.feign")
@EnableScheduling
public class OrderServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderServiceApplication.class, args);
        System.out.println("""

                ======================================
                   智慧旅游助手 - 订单服务
                   Order Service Started!
                ======================================
                """);
    }
}
