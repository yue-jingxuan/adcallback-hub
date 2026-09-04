package com.huida.callbackhub.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 平台配置 Redis 缓存 TTL，对应 {@code huida.callback.cache}。
 */
@Data
@ConfigurationProperties(prefix = "huida.callback.cache")
public class HuidaCallbackCacheProperties {

    /** 单条 id / platformCode 缓存过期时长（秒） */
    private long singleTtl = 1800;

    /** 列表缓存过期时长（秒） */
    private long listTtl = 480;

    /** 空值占位缓存过期时长（秒），用于防御缓存穿透 */
    private long nullTtl = 180;
}
