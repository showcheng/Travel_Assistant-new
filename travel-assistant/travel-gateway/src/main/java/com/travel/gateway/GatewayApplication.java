package com.travel.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 网关启动类
 */
@SpringBootApplication
public class GatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
        System.out.println("======================================");
        System.out.println("   智慧旅游助手平台 - 网关服务");
        System.out.println("   Gateway Service Started!");
        System.out.println("======================================");
    }
}
