package com.datamaster.module.ontology.controller.admin.function.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import javax.validation.constraints.NotNull;

@Schema(description = "函数执行 Request VO")
@Data
public class FunctionExecReqVO {
    @Schema(description = "函数ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "函数ID不能为空")
    private Long functionId;
    @Schema(description = "输入参数JSON")
    private String inputParams;
}
