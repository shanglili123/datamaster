package com.datamaster.module.ontology.controller.admin.action.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;

@Schema(description = "执行提交 Request VO")
@Data
public class ExecutionSubmitReqVO {

    @Schema(description = "动作ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "动作ID不能为空")
    private Long actionId;

    @Schema(description = "输入参数JSON", example = "{\"name\":\"张三\",\"age\":30}")
    private String inputParams;
}
