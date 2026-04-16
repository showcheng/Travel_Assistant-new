package com.travel.common.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 配置
 */
@Configuration
@MapperScan("com.travel.*.mapper")
public class MybatisPlusConfig {

    // 简化配置，使用 MyBatis-Plus 默认配置
    // TODO: 后续添加分页、乐观锁等插件配置
}
