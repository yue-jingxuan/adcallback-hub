package com.huida.callbackhub.cache;

import com.huida.callbackhub.config.HuidaCallbackCacheProperties;
import com.huida.callbackhub.constant.CacheConstants;
import com.huida.callbackhub.entity.PlatformConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * 平台配置手动 Redis 缓存。
 * <p>
 * 单条、列表、空值占位分别使用 yml 中的 single-ttl / list-ttl / null-ttl。
 * 写操作通过 {@link #evictAfterCommit} 在事务提交后再删 Key，避免提交前被旧数据回填。
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PlatformConfigCache {

    /** 空值占位，与真实对象、空列表区分开 */
    static final String NULL_PLACEHOLDER = "__NULL__";

    private final RedisTemplate<String, Object> redisTemplate;
    private final HuidaCallbackCacheProperties cacheProperties;

    /**
     * 读取单条缓存。
     *
     * @return {@link CacheRead#miss()} 表示未命中；hit 且 value 为 null 表示已缓存空值
     */
    public CacheRead<PlatformConfig> getSingle(String key) {
        Object cached = redisTemplate.opsForValue().get(key);
        if (cached == null) {
            return CacheRead.miss();
        }
        if (NULL_PLACEHOLDER.equals(cached)) {
            return CacheRead.hit(null);
        }
        if (cached instanceof PlatformConfig config) {
            return CacheRead.hit(config);
        }
        return CacheRead.miss();
    }

    /**
     * 读取列表缓存。空列表走空值占位，不会按 list-ttl 长期缓存。
     */
    public CacheRead<List<PlatformConfig>> getList(String key) {
        Object cached = redisTemplate.opsForValue().get(key);
        if (cached == null) {
            return CacheRead.miss();
        }
        if (NULL_PLACEHOLDER.equals(cached)) {
            return CacheRead.hit(Collections.emptyList());
        }
        List<PlatformConfig> list = asConfigList(cached);
        if (list == null) {
            return CacheRead.miss();
        }
        return CacheRead.hit(list);
    }

    /**
     * 写入单条配置，同时回填 id、platformCode 两个 Key。
     */
    public void putSingle(PlatformConfig config) {
        if (config == null) {
            return;
        }
        Duration ttl = Duration.ofSeconds(cacheProperties.getSingleTtl());
        if (config.getId() != null) {
            redisTemplate.opsForValue().set(CacheConstants.idKey(config.getId()), config, ttl);
        }
        if (StringUtils.hasText(config.getPlatformCode())) {
            redisTemplate.opsForValue().set(CacheConstants.codeKey(config.getPlatformCode()), config, ttl);
        }
    }

    /**
     * 缓存单条空值，防御按 id / platformCode 的缓存穿透。
     */
    public void putSingleNull(String key) {
        redisTemplate.opsForValue().set(key, NULL_PLACEHOLDER, Duration.ofSeconds(cacheProperties.getNullTtl()));
    }

    /**
     * 写入列表；空列表改用 null-ttl 占位，避免把「库空」按列表 TTL 缓存过久。
     */
    public void putList(String key, List<PlatformConfig> list) {
        if (list == null || list.isEmpty()) {
            redisTemplate.opsForValue().set(key, NULL_PLACEHOLDER, Duration.ofSeconds(cacheProperties.getNullTtl()));
            return;
        }
        redisTemplate.opsForValue().set(key, new ArrayList<>(list), Duration.ofSeconds(cacheProperties.getListTtl()));
    }

    /**
     * 事务提交后再清理与该配置相关的缓存；无事务时立即清理。
     */
    public void evictAfterCommit(PlatformConfig current, PlatformConfig old) {
        runAfterCommit(() -> doEvict(current, old));
    }

    /**
     * 事务提交后按前缀删除全部平台配置缓存（Wrapper 批量更新等无法预知 Key 时使用）。
     */
    public void evictAllAfterCommit() {
        runAfterCommit(this::doEvictAll);
    }

    private void runAfterCommit(Runnable action) {
        if (TransactionSynchronizationManager.isSynchronizationActive()
                && TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    action.run();
                }
            });
            return;
        }
        action.run();
    }

    private void doEvict(PlatformConfig current, PlatformConfig old) {
        List<String> keys = new ArrayList<>();
        addIdAndCodeKeys(keys, current);
        addIdAndCodeKeys(keys, old);
        keys.add(CacheConstants.KEY_LIST);
        keys.add(CacheConstants.KEY_ENABLED_LIST);
        redisTemplate.delete(keys);
        log.debug("已清理平台配置缓存, keys={}", keys);
    }

    private void doEvictAll() {
        Set<String> keys = redisTemplate.keys(CacheConstants.KEY_PATTERN);
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
            log.debug("已按前缀清理平台配置缓存, size={}", keys.size());
        }
    }

    private void addIdAndCodeKeys(List<String> keys, PlatformConfig config) {
        if (config == null) {
            return;
        }
        if (config.getId() != null) {
            keys.add(CacheConstants.idKey(config.getId()));
        }
        if (StringUtils.hasText(config.getPlatformCode())) {
            keys.add(CacheConstants.codeKey(config.getPlatformCode()));
        }
    }

    private List<PlatformConfig> asConfigList(Object cached) {
        if (!(cached instanceof List<?> raw)) {
            return null;
        }
        List<PlatformConfig> result = new ArrayList<>(raw.size());
        for (Object item : raw) {
            if (!(item instanceof PlatformConfig config)) {
                return null;
            }
            result.add(config);
        }
        return result;
    }

    /**
     * 缓存读取结果：miss 表示 Redis 无数据，需回源；hit 表示已命中（value 可为 null / 空列表）。
     */
    public record CacheRead<T>(boolean hit, T value) {

        public static <T> CacheRead<T> miss() {
            return new CacheRead<>(false, null);
        }

        public static <T> CacheRead<T> hit(T value) {
            return new CacheRead<>(true, value);
        }
    }
}
