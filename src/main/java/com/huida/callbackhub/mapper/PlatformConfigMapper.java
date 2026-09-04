package com.huida.callbackhub.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.huida.callbackhub.entity.PlatformConfig;
import org.apache.ibatis.annotations.Mapper;

/**
 * 投流平台配置 Mapper。
 * <p>
 * 继承 MyBatis-Plus {@link BaseMapper}，提供 platform_config 表的基础 CRUD。
 * </p>
 */
@Mapper
public interface PlatformConfigMapper extends BaseMapper<PlatformConfig> {
}
