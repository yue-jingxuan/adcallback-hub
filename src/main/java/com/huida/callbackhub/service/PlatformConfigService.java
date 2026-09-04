package com.huida.callbackhub.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.huida.callbackhub.entity.PlatformConfig;

import java.util.List;

/**
 * 投流平台配置业务接口。
 * <p>
 * 查询走手动 Redis 缓存；新增 / 修改 / 删除在事务提交后清理对应缓存。
 * 请优先使用本接口的 {@code createConfig} / {@code updateConfig} / {@code removeConfig}；
 * 若调用 IService 的 {@code save} / {@code updateById} / {@code removeById}，实现类已覆盖并同样清缓存。
 * </p>
 */
public interface PlatformConfigService extends IService<PlatformConfig> {

    /**
     * 按主键查询平台配置（优先 Redis）。
     *
     * @param id 主键
     * @return 配置，不存在时返回 {@code null}
     */
    PlatformConfig getConfigById(Long id);

    /**
     * 按平台编码查询，回调验签与转发的主查询路径。
     *
     * @param platformCode 平台编码，如 douyin
     * @return 配置，不存在时返回 {@code null}
     */
    PlatformConfig getByPlatformCode(String platformCode);

    /**
     * 查询全部平台配置（优先 Redis）。
     *
     * @return 配置列表，不会返回 {@code null}
     */
    List<PlatformConfig> listAllCached();

    /**
     * 查询已启用的平台配置（优先 Redis）。
     *
     * @return 启用中的配置列表
     */
    List<PlatformConfig> listEnabled();

    /**
     * 新增平台配置，成功后清理列表缓存。
     *
     * @param config 待保存配置
     * @return 是否保存成功
     */
    boolean createConfig(PlatformConfig config);

    /**
     * 按主键更新平台配置，成功后清理 id / code / 列表缓存。
     *
     * @param config 待更新配置，必须包含 id
     * @return 是否更新成功
     */
    boolean updateConfig(PlatformConfig config);

    /**
     * 按主键删除平台配置，成功后清理对应缓存。
     *
     * @param id 主键
     * @return 是否删除成功
     */
    boolean removeConfig(Long id);
}
