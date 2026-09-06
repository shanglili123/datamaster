package com.datamaster.module.ontology.controller.admin.webhook.vo;

import com.datamaster.common.core.page.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema(description = "Webhook 回调日志分页查询 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class WebhookLogPageReqVO extends PageParam {

    private static final long serialVersionUID = 1L;

    @Schema(description = "Webhook配置ID", example = "1")
    private Long webhookId;

    @Schema(description = "执行类型：ACTION", example = "ACTION")
    private String executionType;

    @Schema(description = "动作执行记录ID", example = "1")
    private Long actionExecutionId;

    @Schema(description = "回调状态：SUCCESS / FAILED / PENDING", example = "SUCCESS")
    private String status;
}