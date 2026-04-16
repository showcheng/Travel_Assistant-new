package com.travel.group;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 拼团服务启动类
 */
@SpringBootApplication(scanBasePackages = "com.travel")
@MapperScan("com.travel.group.mapper")
public class GroupBuyServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(GroupBuyServiceApplication.class, args);
        System.out.println("""

                ======================================
                   智慧旅游助手 - 拼团服务
                   Group Buy Service Started!
                ======================================
                """);
    }
}
