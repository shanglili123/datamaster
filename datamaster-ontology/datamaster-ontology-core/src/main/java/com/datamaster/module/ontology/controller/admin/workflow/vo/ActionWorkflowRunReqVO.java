package com.datamaster.module.ontology.controller.admin.workflow.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "启动动作编排 Request VO")
public class ActionWorkflowRunReqVO {
    @Schema(description = "运行输入 JSON")
    private String inputParams;
    @Schema(description = "可选：启动时要求匹配的已发布定义版本")
    private Integer expectedVersion;
    @Schema(description = "当前对象主键")
    private String objectKey;
    @Schema(description = "空间 ID")
    private Long spaceId;
    @Schema(description = "空间编码")
    private String spaceCode;
    @Schema(description = "触发事件唯一标识")
    private String eventId;
    @Schema(description = "Workflow 运行幂等键")
    private String idempotencyKey;
    @Schema(description = "是否异步提交；true 时由 Workflow Worker 后台运行")
    private Boolean async;
}
