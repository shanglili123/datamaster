package com.datamaster.module.ontology.controller.admin.function.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import javax.validation.constraints.NotNull;

@Schema(description = "函数审批 Request VO")
@Data
public class FunctionApprovalReqVO {
    @Schema(description = "执行记录ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "执行记录ID不能为空")
    private Long executionId;
    @Schema(description = "审批意见")
    private String approvalReason;
}
