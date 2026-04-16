package com.travel.product;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 商品服务启动类
 */
@SpringBootApplication(scanBasePackages = "com.travel")
@MapperScan("com.travel.product.mapper")
public class ProductServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProductServiceApplication.class, args);
        System.out.println("""

                ======================================
                   智慧旅游助手 - 商品服务
                   Product Service Started!
                ======================================
                """);
    }
}
