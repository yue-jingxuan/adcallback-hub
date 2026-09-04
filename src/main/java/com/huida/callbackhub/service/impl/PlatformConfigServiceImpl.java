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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 投流平台配置业务实现。
 * <p>
 * 读走手动 Redis 缓存；重写Mybatis‑Plus原生查询方法，强制走缓存。
 * 写操作先落库，在事务提交后再清理对应 Key。
 * 缓存清单：id单条缓存、platformCode单条缓存、全量列表缓存、已启用列表缓存。
 * 任何平台配置发生变更，统一失效【已启用列表缓存】KEY_ENABLED_LIST。
 * listEnabled 查询条件仅判断 enabled=1，webhookEnabled 不参与该过滤条件。
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
     * 根据主键查询（优先Redis缓存）
     *
     * @param id 主键id
     * @return 平台配置，不存在返回null
     */
    @Override
    public PlatformConfig getConfigById(Long id) {
        if (id == null) {
            return null;
        }
        String key = CacheConstants.idKey(id);
        CacheRead<PlatformConfig> cached = platformConfigCache.getSingle(key);
        // 缓存命中直接返回
        if (cached.hit()) {
            return cached.value();
        }
        // 缓存未命中查询数据库
        PlatformConfig db = super.getById(id);
        if (db == null) {
            // 查询为空，缓存空值防止缓存击穿，空缓存务必配置较短TTL
            platformConfigCache.putSingleNull(key);
            return null;
        }
        platformConfigCache.putSingle(db);
        return db;
    }

    /**
     * 重写MP原生getById，路由至带缓存查询
     */
    @Override
    public PlatformConfig getById(Serializable id) {
        Long longId = toLongId(id);
        if (longId == null) {
            return super.getById(id);
        }
        return getConfigById(longId);
    }

    @Override
    public Optional<PlatformConfig> getOptById(Serializable id) {
        return Optional.ofNullable(getById(id));
    }

    /**
     * 根据平台编码查询，网关回调主查询入口
     *
     * @param platformCode 平台编码 douyin / kuaishou
     * @return 平台配置
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
     * 查询全部平台配置（优先缓存）
     */
    @Override
    public List<PlatformConfig> listAll() {
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
     * 重写MP原生list，全部查询强制走缓存
     */
    @Override
    public List<PlatformConfig> list() {
        return listAll();
    }

    /**
     * 批量根据id查询，优先命中单条缓存，未命中批量查库回写缓存
     * <p>
     * 注意：入参携带重复id，返回列表将保留重复id顺序，与MP原生IN查询自动去重行为不一致。
     * </p>
     */
    @Override
    public List<PlatformConfig> listByIds(Collection<? extends Serializable> idList) {
        if (idList == null || idList.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> orderedIds = new ArrayList<>();
        for (Serializable id : idList) {
            Long longId = toLongId(id);
            if (longId != null) {
                orderedIds.add(longId);
            }
        }

        Map<Long, PlatformConfig> found = new LinkedHashMap<>();
        List<Long> misses = new ArrayList<>();

        // 先读取缓存
        for (Long id : orderedIds) {
            CacheRead<PlatformConfig> cached = platformConfigCache.getSingle(CacheConstants.idKey(id));
            if (cached.hit()) {
                if (cached.value() != null) {
                    found.put(id, cached.value());
                }
            } else {
                misses.add(id);
            }
        }

        // 缓存未命中部分查询数据库，回写缓存
        if (!misses.isEmpty()) {
            for (PlatformConfig config : super.listByIds(misses)) {
                platformConfigCache.putSingle(config);
                found.put(config.getId(), config);
            }
            // 数据库不存在id，写入空缓存
            for (Long miss : misses) {
                if (!found.containsKey(miss)) {
                    platformConfigCache.putSingleNull(CacheConstants.idKey(miss));
                }
            }
        }

        // 保持传入id顺序返回结果
        List<PlatformConfig> result = new ArrayList<>();
        for (Long id : orderedIds) {
            PlatformConfig config = found.get(id);
            if (config != null) {
                result.add(config);
            }
        }
        return result;
    }

    /**
     * 查询所有已启用的平台配置（优先缓存）
     */
    @Override
    public List<PlatformConfig> listEnabled() {
        CacheRead<List<PlatformConfig>> cached = platformConfigCache.getList(CacheConstants.KEY_ENABLED_LIST);
        if (cached.hit()) {
            return cached.value();
        }
        List<PlatformConfig> list = lambdaQuery()
                .eq(PlatformConfig::getEnabled, 1)
                .list();
        List<PlatformConfig> result = list == null ? Collections.emptyList() : list;
        platformConfigCache.putList(CacheConstants.KEY_ENABLED_LIST, result);
        return result;
    }

    /**
     * 新增平台配置，增加platformCode唯一性校验
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean createConfig(PlatformConfig config) {
        Objects.requireNonNull(config, "平台配置不能为空");
        if (!StringUtils.hasText(config.getPlatformCode())) {
            throw new IllegalArgumentException("平台编码不能为空");
        }
        // 查重校验，防止重复平台编码；数据库需配套建立唯一索引做并发兜底
        PlatformConfig exist = getByPlatformCode(config.getPlatformCode());
        if (exist != null) {
            throw new IllegalArgumentException("该平台编码已存在：" + config.getPlatformCode());
        }
        // 设置默认值
        if (config.getEnabled() == null) {
            config.setEnabled(1);
        }
        if (config.getWebhookEnabled() == null) {
            config.setWebhookEnabled(0);
        }
        boolean saved = save(config);
        if (saved) {
            log.info("新增平台配置成功, id={}, platformCode={}", config.getId(), config.getPlatformCode());
        }
        return saved;
    }

    /**
     * 更新平台配置，变更平台编码时校验唯一性
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateConfig(PlatformConfig config) {
        Objects.requireNonNull(config, "平台配置不能为空");
        if (config.getId() == null) {
            throw new IllegalArgumentException("更新平台配置时主键不能为空");
        }
        // 变更平台编码时查重，排除自身，防止改成已存在的编码；数据库唯一索引做并发兜底
        if (StringUtils.hasText(config.getPlatformCode())) {
            PlatformConfig exist = getByPlatformCode(config.getPlatformCode());
            if (exist != null && !config.getId().equals(exist.getId())) {
                throw new IllegalArgumentException("该平台编码已存在：" + config.getPlatformCode());
            }
        }
        boolean updated = updateById(config);
        if (updated) {
            log.info("更新平台配置成功, id={}, platformCode={}", config.getId(), config.getPlatformCode());
        }
        return updated;
    }

    /**
     * 删除平台配置
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeConfig(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("删除平台配置时主键不能为空");
        }
        PlatformConfig oldConfig = super.getById(id);
        if (oldConfig == null) {
            return false;
        }
        // 直调 super 删除，复用已查出的 oldConfig 清缓存，避免 removeById 内部二次查库
        boolean removed = super.removeById(id);
        if (removed) {
            platformConfigCache.evictAfterCommit(oldConfig, null);
            log.info("删除平台配置成功, id={}, platformCode={}", id, oldConfig.getPlatformCode());
        }
        return removed;
    }

    // ====================== 重写 Mybatis‑Plus 内置写方法 ======================

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
    public boolean saveBatch(Collection<PlatformConfig> entityList, int batchSize) {
        boolean saved = super.saveBatch(entityList, batchSize);
        if (saved && entityList != null) {
            entityList.forEach(item -> platformConfigCache.evictAfterCommit(item, null));
        }
        return saved;
    }

    /**
     * 重写saveOrUpdate，存在性判断直查数据库不走缓存，消除数据源不一致风险
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveOrUpdate(PlatformConfig entity) {
        boolean exist;
        if (entity == null || entity.getId() == null) {
            exist = false;
        } else {
            exist = super.getById(entity.getId()) != null;
        }
        if (exist) {
            return updateById(entity);
        } else {
            return save(entity);
        }
    }

    /**
     * 修复：循环调用本地saveOrUpdate，禁止MP原生存在性判断读取缓存
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveOrUpdateBatch(Collection<PlatformConfig> entityList, int batchSize) {
        if (entityList == null || entityList.isEmpty()) {
            return false;
        }
        for (PlatformConfig item : entityList) {
            this.saveOrUpdate(item);
        }
        platformConfigCache.evictAllAfterCommit();
        return true;
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
    public boolean updateBatchById(Collection<PlatformConfig> entityList, int batchSize) {
        if (entityList == null || entityList.isEmpty()) {
            return false;
        }
        List<Long> ids = entityList.stream()
                .map(PlatformConfig::getId)
                .filter(Objects::nonNull)
                .toList();
        Map<Long, PlatformConfig> oldMap = ids.isEmpty()
                ? Collections.emptyMap()
                : super.listByIds(ids).stream()
                .collect(Collectors.toMap(PlatformConfig::getId, Function.identity(), (a, b) -> a));

        boolean updated = super.updateBatchById(entityList, batchSize);
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
        return removeById(id, false);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeById(Serializable id, boolean useFill) {
        PlatformConfig old = super.getById(id);
        boolean removed = super.removeById(id, useFill);
        if (removed && old != null) {
            platformConfigCache.evictAfterCommit(old, null);
        }
        return removed;
    }

    /**
     * 修复高危bug：入参实体可能残缺，必须查库拿到完整旧对象再清理缓存
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeById(PlatformConfig entity) {
        if (entity == null || entity.getId() == null) {
            return false;
        }
        PlatformConfig old = super.getById(entity.getId());
        if(old == null){
            return false;
        }
        boolean removed = super.removeById(entity);
        if (removed) {
            platformConfigCache.evictAfterCommit(old, null);
        }
        return removed;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeByIds(Collection<?> list) {
        return removeByIds(list, false);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeByIds(Collection<?> list, boolean useFill) {
        if (list == null || list.isEmpty()) {
            return false;
        }
        List<Serializable> ids = toSerializableIds(list);
        List<PlatformConfig> olds = ids.isEmpty() ? Collections.emptyList() : super.listByIds(ids);
        boolean removed = super.removeByIds(list, useFill);
        if (removed) {
            olds.forEach(item -> platformConfigCache.evictAfterCommit(item, null));
        }
        return removed;
    }

    /**
     * MP 3.5.9 IService 仅有此一个 removeBatchByIds 重载，委托至带缓存清理的 removeByIds
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeBatchByIds(Collection<?> list) {
        return removeByIds(list, false);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeByMap(Map<String, Object> columnMap) {
        boolean removed = super.removeByMap(columnMap);
        if (removed) {
            platformConfigCache.evictAllAfterCommit();
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

    /**
     * Serializable id转Long
     */
    private Long toLongId(Serializable id) {
        if (id == null) {
            return null;
        }
        if (id instanceof Long longId) {
            return longId;
        }
        if (id instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(id.toString());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private List<Serializable> toSerializableIds(Collection<?> list) {
        List<Serializable> ids = new ArrayList<>();
        for (Object id : list) {
            if (id instanceof Serializable serializable) {
                ids.add(serializable);
            }
        }
        return ids;
    }
}
