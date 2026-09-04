package com.huida.callbackhub.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 回调请求日志实体，对应表 {@code callback_log}。
 * <p>
 * 记录各广告平台回调的原始报文、转发状态与重试信息，便于排查与补偿。
 * </p>
 */
@Data
@TableName("callback_log")
public class CallbackLog implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 平台编码：douyin / kuaishou / chuanshanjia */
    private String platform;

    /** 广告点击 ID */
    private String clickId;

    /** 回调原始请求报文 */
    private String rawBody;

    /** 状态：pending 待转发、success 成功、failed 失败 */
    private String status;

    /** 已重试次数 */
    private Integer retryCount;

    /** 最大重试次数 */
    private Integer maxRetry;

    /** 失败错误信息 */
    private String errorMsg;

    /** 链路追踪 ID */
    private String traceId;

    /** 下游转发地址 */
    private String targetUrl;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}
