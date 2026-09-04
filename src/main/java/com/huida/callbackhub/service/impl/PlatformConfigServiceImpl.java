package com.huida.callbackhub.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.huida.callbackhub.cache.PlatformConfigCache;
import com.huida.callbackhub.cache.PlatformConfigCache.CacheRead;
import com.huida.callbackhub.constant.CacheConstants;
import com.huida.callbackhub.entity.PlatformConfig;
import com.huida.callbackhub.mapper.PlatformConfigMapper;
import com.huida.callbackhub.service.PlatformConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 投流平台配置业务实现。
 * <p>
 * 读走手动 Redis 缓存；写操作先落库，在事务提交后再清理对应 Key。
 * 覆盖 IService 写方法，避免调用方走 {@code updateById} / {@code removeById} 时漏清缓存。
 * </p>
 */
@Slf4j
@Service
public class PlatformConfigServiceImpl extends ServiceImpl<PlatformConfigMapper, PlatformConfig>
        implements PlatformConfigService {

    private final PlatformConfigCache platformConfigCache;

    public PlatformConfigServiceImpl(PlatformConfigCache platformConfigCache) {
        this.platformConfigCache = platformConfigCache;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PlatformConfig getConfigById(Long id) {
        if (id == null) {
            return null;
        }
        String key = CacheConstants.idKey(id);
        CacheRead<PlatformConfig> cached = platformConfigCache.getSingle(key);
        if (cached.hit()) {
            return cached.value();
        }
        PlatformConfig db = super.getById(id);
        if (db == null) {
            platformConfigCache.putSingleNull(key);
            return null;
        }
        platformConfigCache.putSingle(db);
        return db;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PlatformConfig getByPlatformCode(String platformCode) {
        if (!StringUtils.hasText(platformCode)) {
            return null;
        }
        String key = CacheConstants.codeKey(platformCode);
        CacheRead<PlatformConfig> cached = platformConfigCache.getSingle(key);
        if (cached.hit()) {
            return cached.value();
        }
        PlatformConfig db = lambdaQuery()
                .eq(PlatformConfig::getPlatformCode, platformCode)
                .one();
        if (db == null) {
            platformConfigCache.putSingleNull(key);
            return null;
        }
        platformConfigCache.putSingle(db);
        return db;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<PlatformConfig> listAllCached() {
        CacheRead<List<PlatformConfig>> cached = platformConfigCache.getList(CacheConstants.KEY_LIST);
        if (cached.hit()) {
            return cached.value();
        }
        List<PlatformConfig> list = super.list();
        List<PlatformConfig> result = list == null ? Collections.emptyList() : list;
        platformConfigCache.putList(CacheConstants.KEY_LIST, result);
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<PlatformConfig> listEnabled() {
        CacheRead<List<PlatformConfig>> cached = platformConfigCache.getList(CacheConstants.KEY_ENABLED_LIST);
        if (cached.hit()) {
            return cached.value();
        }
        List<PlatformConfig> list = lambdaQuery()
                .eq(PlatformConfig::getEnable, 1)
                .list();
        List<PlatformConfig> result = list == null ? Collections.emptyList() : list;
        platformConfigCache.putList(CacheConstants.KEY_ENABLED_LIST, result);
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean createConfig(PlatformConfig config) {
        Objects.requireNonNull(config, "平台配置不能为空");
        if (!StringUtils.hasText(config.getPlatformCode())) {
            throw new IllegalArgumentException("平台编码不能为空");
        }
        if (!StringUtils.hasText(config.getTargetUrl())) {
            throw new IllegalArgumentException("下游转发地址不能为空");
        }
        if (config.getEnable() == null) {
            config.setEnable(1);
        }
        boolean saved = save(config);
        if (saved) {
            log.info("新增平台配置成功, id={}, platformCode={}", config.getId(), config.getPlatformCode());
        }
        return saved;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateConfig(PlatformConfig config) {
        Objects.requireNonNull(config, "平台配置不能为空");
        if (config.getId() == null) {
            throw new IllegalArgumentException("更新平台配置时主键不能为空");
        }
        boolean updated = updateById(config);
        if (updated) {
            log.info("更新平台配置成功, id={}, platformCode={}", config.getId(), config.getPlatformCode());
        }
        return updated;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeConfig(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("删除平台配置时主键不能为空");
        }
        PlatformConfig oldConfig = super.getById(id);
        boolean removed = removeById(id);
        if (removed) {
            log.info("删除平台配置成功, id={}, platformCode={}", id,
                    oldConfig == null ? null : oldConfig.getPlatformCode());
        }
        return removed;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean save(PlatformConfig entity) {
        boolean saved = super.save(entity);
        if (saved) {
            platformConfigCache.evictAfterCommit(entity, null);
        }
        return saved;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveBatch(Collection<PlatformConfig> entityList) {
        boolean saved = super.saveBatch(entityList);
        if (saved && entityList != null) {
            entityList.forEach(item -> platformConfigCache.evictAfterCommit(item, null));
        }
        return saved;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveOrUpdate(PlatformConfig entity) {
        PlatformConfig old = entity != null && entity.getId() != null ? super.getById(entity.getId()) : null;
        boolean ok = super.saveOrUpdate(entity);
        if (ok) {
            platformConfigCache.evictAfterCommit(entity, old);
        }
        return ok;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveOrUpdateBatch(Collection<PlatformConfig> entityList) {
        boolean ok = super.saveOrUpdateBatch(entityList);
        if (ok) {
            platformConfigCache.evictAllAfterCommit();
        }
        return ok;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateById(PlatformConfig entity) {
        if (entity == null || entity.getId() == null) {
            return false;
        }
        PlatformConfig old = super.getById(entity.getId());
        if (old == null) {
            return false;
        }
        boolean updated = super.updateById(entity);
        if (updated) {
            platformConfigCache.evictAfterCommit(entity, old);
        }
        return updated;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateBatchById(Collection<PlatformConfig> entityList) {
        if (entityList == null || entityList.isEmpty()) {
            return false;
        }
        List<Long> ids = entityList.stream()
                .map(PlatformConfig::getId)
                .filter(Objects::nonNull)
                .toList();
        Map<Long, PlatformConfig> oldMap = super.listByIds(ids).stream()
                .collect(Collectors.toMap(PlatformConfig::getId, Function.identity(), (a, b) -> a));
        boolean updated = super.updateBatchById(entityList);
        if (updated) {
            entityList.forEach(item -> platformConfigCache.evictAfterCommit(item, oldMap.get(item.getId())));
        }
        return updated;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean update(Wrapper<PlatformConfig> updateWrapper) {
        boolean updated = super.update(updateWrapper);
        if (updated) {
            platformConfigCache.evictAllAfterCommit();
        }
        return updated;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean update(PlatformConfig entity, Wrapper<PlatformConfig> updateWrapper) {
        boolean updated = super.update(entity, updateWrapper);
        if (updated) {
            platformConfigCache.evictAllAfterCommit();
        }
        return updated;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeById(Serializable id) {
        PlatformConfig old = super.getById(id);
        boolean removed = super.removeById(id);
        if (removed) {
            platformConfigCache.evictAfterCommit(old, null);
        }
        return removed;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeById(PlatformConfig entity) {
        PlatformConfig old = entity != null && entity.getId() != null ? super.getById(entity.getId()) : entity;
        boolean removed = super.removeById(entity);
        if (removed) {
            platformConfigCache.evictAfterCommit(old, null);
        }
        return removed;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeByIds(Collection<?> list) {
        if (list == null || list.isEmpty()) {
            return false;
        }
        List<Serializable> ids = list.stream()
                .filter(Serializable.class::isInstance)
                .map(Serializable.class::cast)
                .toList();
        List<PlatformConfig> olds = ids.isEmpty() ? Collections.emptyList() : super.listByIds(ids);
        boolean removed = super.removeByIds(list);
        if (removed) {
            olds.forEach(item -> platformConfigCache.evictAfterCommit(item, null));
        }
        return removed;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeBatchByIds(Collection<?> list) {
        if (list == null || list.isEmpty()) {
            return false;
        }
        List<Serializable> ids = list.stream()
                .filter(Serializable.class::isInstance)
                .map(Serializable.class::cast)
                .toList();
        List<PlatformConfig> olds = ids.isEmpty() ? Collections.emptyList() : super.listByIds(ids);
        boolean removed = super.removeBatchByIds(list);
        if (removed) {
            olds.forEach(item -> platformConfigCache.evictAfterCommit(item, null));
        }
        return removed;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean remove(Wrapper<PlatformConfig> queryWrapper) {
        List<PlatformConfig> olds = super.list(queryWrapper);
        boolean removed = super.remove(queryWrapper);
        if (removed) {
            olds.forEach(item -> platformConfigCache.evictAfterCommit(item, null));
        }
        return removed;
    }
}
