package com.datamaster.module.ontology.controller.admin.action.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

@Schema(description = "审批 Request VO")
@Data
public class ApprovalReqVO {

    @Schema(description = "执行记录ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "执行记录ID不能为空")
    private Long executionId;

    @Schema(description = "审批意见（可选，永久审计）", example = "已人工确认")
    private String approvalReason;

    @Schema(description = "审批结论：approve=同意 / reject=拒绝（区分两接口，仅作审计冗余）", hidden = true)
    @Pattern(regexp = "approve|reject", message = "审批结论仅 accept approve/reject")
    private String decision;
}
