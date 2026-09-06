package com.datamaster.module.ontology.dal.dataobject;

import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.datamaster.common.core.domain.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Webhook 回调日志 DO
 *
 * <p>对应表 ONT_WEBHOOK_LOG。记录每次针对「动作执行记录」发起的回调明细，
 * 用于回溯、排查与重试。</p>
 */
@Data
@TableName("ONT_WEBHOOK_LOG")
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class WebhookLogDO extends BaseEntity {

    /** Webhook 配置ID */
    private Long webhookId;

    /** Webhook 名称（冗余，便于检索） */
    private String webhookName;

    /** 触发来源执行类型：ACTION */
    private String executionType;

    /** 动作执行记录ID */
    private Long actionExecutionId;

    /** 动作所在本体ID */
    private Long ontologyId;

    /** 回调请求体（基于执行记录生成的 payload） */
    private String payload;

    /** 回调状态：SUCCESS / FAILED / PENDING（重试中） */
    private String status;

    /** HTTP 状态码（请求已发出时） */
    private Integer httpStatus;

    /** 响应体 */
    private String responseBody;

    /** 错误信息 */
    private String errorMessage;

    /** 已重试次数 */
    private Integer retryCount;

    @TableLogic
    private Integer delFlag;
}