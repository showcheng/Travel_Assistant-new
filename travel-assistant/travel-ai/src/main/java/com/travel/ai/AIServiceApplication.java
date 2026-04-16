package com.travel.ai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * AI 服务启动类
 */
@SpringBootApplication(scanBasePackages = "com.travel")
public class AIServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AIServiceApplication.class, args);
        System.out.println("""

                ======================================
                   智慧旅游助手 - AI 服务
                   AI Service Started!
                ======================================
                """);
    }
}
