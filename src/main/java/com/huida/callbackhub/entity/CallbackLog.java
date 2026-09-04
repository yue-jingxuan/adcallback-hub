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
 * 同时覆盖 CAPI 出站上报（mode_type=1）与 Webhook 入站转发（mode_type=2）。
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

    /** 事件唯一 ID，用于幂等，对应 uk_event_id */
    private String eventId;

    /** 模式：1-CAPI 出站上报，2-Webhook 入站转发 */
    private Integer modeType;

    /** 广告点击 ID */
    private String clickId;

    /** 原始请求报文 */
    private String rawBody;

    /** CAPI 上报请求报文（模式 1） */
    private String capiRequest;

    /** CAPI 上报响应报文（模式 1） */
    private String capiResponse;

    /** 状态：pending 待处理、success 成功、failed 失败、dead 死信 */
    private String status;

    /** 已重试次数 */
    private Integer retryCount;

    /** 失败错误信息 */
    private String errorMsg;

    /** 链路追踪 ID */
    private String traceId;

    /** 下游转发地址（模式 2） */
    private String targetUrl;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}
