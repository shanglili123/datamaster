package com.datamaster.module.ontology.controller.admin.function.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Schema(description = "函数保存 Request VO")
@Data
public class FunctionSaveReqVO {
    @Schema(description = "编号")
    private Long id;
    @Schema(description = "本体ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "本体ID不能为空")
    private Long ontologyId;
    @Schema(description = "函数名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "函数名称不能为空")
    private String name;
    @Schema(description = "函数编码", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "函数编码不能为空")
    private String code;
    @Schema(description = "语言：TYPESCRIPT/PYTHON", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "语言不能为空")
    private String lang;
    @Schema(description = "函数代码体", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "代码体不能为空")
    private String body;
    @Schema(description = "是否需要审批")
    private Boolean needsApproval;
    @Schema(description = "描述")
    private String description;
}
