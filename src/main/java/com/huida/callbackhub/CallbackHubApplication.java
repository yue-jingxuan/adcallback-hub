package com.huida.callbackhub;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * 汇答投流回调网关启动类
 * <p>
 * 负责启动 Spring Boot 应用，并开启异步任务能力，
 * 用于广告平台回调的异步验签、落库、转发与重试。
 * 通过 {@code @MapperScan} 统一扫描 mapper 包，所有 Mapper 接口不再需要添加 {@code @Mapper} 注解。
 * </p>
 *
 * @author huida
 */
@SpringBootApplication
@EnableAsync
@MapperScan("com.huida.callbackhub.mapper")
public class CallbackHubApplication {

    /**
     * 应用入口
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(CallbackHubApplication.class, args);
    }
}
