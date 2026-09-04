package com.huida.callbackhub.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.huida.callbackhub.entity.CallbackLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 回调请求日志 Mapper。
 * <p>
 * 继承 MyBatis-Plus {@link BaseMapper}，提供 callback_log 表的基础 CRUD。
 * </p>
 */
@Mapper
public interface CallbackLogMapper extends BaseMapper<CallbackLog> {
}
