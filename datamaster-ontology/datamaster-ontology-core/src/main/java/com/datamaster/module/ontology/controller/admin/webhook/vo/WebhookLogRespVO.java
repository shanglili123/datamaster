package com.datamaster.module.ontology.controller.admin.webhook.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Schema(description = "Webhook 回调日志 Response VO")
@Data
public class WebhookLogRespVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "编号", example = "1")
    private Long id;

    @Schema(description = "Webhook配置ID", example = "1")
    private Long webhookId;

    @Schema(description = "Webhook 名称", example = "客户源系统回调")
    private String webhookName;

    @Schema(description = "执行类型：ACTION", example = "ACTION")
    private String executionType;

    @Schema(description = "动作执行记录ID", example = "1")
    private Long actionExecutionId;

    @Schema(description = "调用时所在本体ID", example = "1")
    private Long ontologyId;

    @Schema(description = "回调请求体")
    private String payload;

    @Schema(description = "回调状态：SUCCESS / FAILED / PENDING", example = "SUCCESS")
    private String status;

    @Schema(description = "HTTP 状态码", example = "200")
    private Integer httpStatus;

    @Schema(description = "响应体")
    private String responseBody;

    @Schema(description = "错误信息")
    private String errorMessage;

    @Schema(description = "已重试次数", example = "0")
    private Integer retryCount;

    @Schema(description = "创建时间", example = "2026-09-03 10:00:00")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;
}