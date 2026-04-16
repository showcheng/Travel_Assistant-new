package com.travel.seckill;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;

/**
 * 秒杀服务启动类
 */
@EnableKafka
@SpringBootApplication(scanBasePackages = "com.travel")
@MapperScan("com.travel.seckill.mapper")
public class SeckillServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(SeckillServiceApplication.class, args);
        System.out.println("""

                ======================================
                   智慧旅游助手 - 秒杀服务
                   Seckill Service Started!
                ======================================
                """);
    }
}
