package com.datamaster.module.ontology.controller.admin.action.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;

@Schema(description = "审批 Request VO")
@Data
public class ApprovalReqVO {

    @Schema(description = "执行记录ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "执行记录ID不能为空")
    private Long executionId;

    @Schema(description = "审批意见", example = "同意")
    private String approvalReason;
}
