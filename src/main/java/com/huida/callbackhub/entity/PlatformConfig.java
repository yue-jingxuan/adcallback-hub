package com.huida.callbackhub.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 投流平台配置实体，对应表 {@code platform_config}。
 * <p>
 * 保存平台验签密钥、默认下游转发地址及启停状态，回调主链路会高频读取。
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

    /** 验签密钥 */
    private String signSecret;

    /** 默认下游转发地址 */
    private String targetUrl;

    /** 是否启用：1 启用，0 关闭 */
    @TableField("enable")
    private Integer enable;

    /** 备注 */
    private String remark;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}
