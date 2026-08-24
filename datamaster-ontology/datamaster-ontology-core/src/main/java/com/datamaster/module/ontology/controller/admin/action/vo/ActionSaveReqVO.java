package com.datamaster.module.ontology.controller.admin.action.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Schema(description = "动作保存 Request VO")
@Data
public class ActionSaveReqVO {

    @Schema(description = "编号，更新时必填", example = "1")
    private Long id;

    @Schema(description = "本体ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "本体ID不能为空")
    private Long ontologyId;

    @Schema(description = "动作名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "创建客户")
    @NotBlank(message = "动作名称不能为空")
    private String name;

    @Schema(description = "动作类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "CREATE")
    @NotBlank(message = "动作类型不能为空")
    private String actionType;

    @Schema(description = "绑定概念ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "绑定概念不能为空")
    private Long conceptId;

    @Schema(description = "描述", example = "创建新客户记录")
    private String description;
}
