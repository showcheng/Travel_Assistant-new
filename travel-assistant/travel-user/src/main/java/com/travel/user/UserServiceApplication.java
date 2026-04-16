package com.travel.user;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 用户服务启动类
 */
@SpringBootApplication(scanBasePackages = "com.travel")
@MapperScan("com.travel.user.mapper")
public class UserServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
        System.out.println("""

                ======================================
                   智慧旅游助手 - 用户服务
                   User Service Started!
                ======================================
                """);
    }
}
