package com.datamaster.module.ontology.controller.admin.webhook.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Schema(description = "Webhook Response VO")
@Data
public class WebhookRespVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "编号", example = "1")
    private Long id;

    @Schema(description = "Webhook 名称", example = "客户源系统回调")
    private String name;

    @Schema(description = "所属本体ID", example = "1")
    private Long ontologyId;

    @Schema(description = "绑定的动作ID", example = "1")
    private Long actionId;

    @Schema(description = "回调地址", example = "http://source-system/api/callback")
    private String url;

    @Schema(description = "请求方法", example = "POST")
    private String method;

    @Schema(description = "请求头（JSON）")
    private String headers;

    @Schema(description = "请求体模板")
    private String payloadTemplate;

    @Schema(description = "回调密钥")
    private String secret;

    @Schema(description = "是否启用", example = "true")
    private Boolean enabled;

    @Schema(description = "失败最大重试次数", example = "2")
    private Integer maxRetry;

    @Schema(description = "描述")
    private String description;

    @Schema(description = "创建者", example = "admin")
    private String createBy;

    @Schema(description = "创建时间", example = "2026-09-03 10:00:00")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;
}