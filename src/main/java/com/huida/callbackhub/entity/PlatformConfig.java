package com.huida.callbackhub.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 投流平台配置实体，对应表 {@code platform_config}。
 * <p>
 * 密钥、token、api_url、超时等放在 {@code config_json}；
 * {@code webhook_enabled} 控制是否接收 Webhook，{@code enabled} 控制平台总开关。
 * </p>
 */
@Data
@TableName("platform_config")
public class PlatformConfig implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 平台编码，如 douyin，库内唯一 */
    private String platformCode;

    /** 平台名称 */
    private String platformName;

    /**
     * 平台配置 JSON：secret、token、api_url、超时等。
     * 对应 MySQL json 列 {@code config_json}。
     */
    private String configJson;

    /** 默认下游转发地址（模式 2 Webhook 转发） */
    private String targetUrl;

    /** 是否开启 Webhook 接收：1 开启，0 关闭（默认） */
    private Integer webhookEnabled;

    /** 是否启用：1 启用，0 禁用 */
    private Integer enabled;

    /** 备注 */
    private String remark;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}
