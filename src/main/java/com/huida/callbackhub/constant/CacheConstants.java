package com.huida.callbackhub.constant;

/**
 * 平台配置 Redis Key。
 */
public final class CacheConstants {

    private CacheConstants() {
    }

    /** 平台配置 Key 前缀 */
    public static final String PLATFORM_CONFIG_PREFIX = "huida:callback:platform-config:";

    /** 按主键，完整 Key 形如 huida:callback:platform-config:id:1 */
    public static final String KEY_ID_PREFIX = PLATFORM_CONFIG_PREFIX + "id:";

    /** 按平台编码，完整 Key 形如 huida:callback:platform-config:code:douyin */
    public static final String KEY_CODE_PREFIX = PLATFORM_CONFIG_PREFIX + "code:";

    /** 全量列表 */
    public static final String KEY_LIST = PLATFORM_CONFIG_PREFIX + "list";

    /** 启用中配置列表 */
    public static final String KEY_ENABLED_LIST = PLATFORM_CONFIG_PREFIX + "enabledList";

    /** 扫描/批量删除时使用的匹配模式 */
    public static final String KEY_PATTERN = PLATFORM_CONFIG_PREFIX + "*";

    public static String idKey(Long id) {
        return KEY_ID_PREFIX + id;
    }

    public static String codeKey(String platformCode) {
        return KEY_CODE_PREFIX + platformCode;
    }
}
