package com.datamaster.module.ontology.controller.admin.aigenerate.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * AI 生成动作预览 Request VO
 */
@Schema(description = "AI 生成动作预览 Request VO")
@Data
public class AiActionPreviewReqVO {

    @Schema(description = "本体ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "本体ID不能为空")
    private Long ontologyId;

    @Schema(description = "触发概念ID（为空则生成整个本体的动作）", example = "1")
    private Long conceptId;

    @Schema(description = "用户对动作功能的自然语言描述", requiredMode = Schema.RequiredMode.REQUIRED,
            example = "帮我创建一个扣减库存的多步骤动作，先检查库存是否充足，再扣减库存数量")
    @NotBlank(message = "动作描述不能为空")
    private String prompt;

    @Schema(description = "期望生成的动作类型过滤，为空则同时生成 COMPOSITE 和 FUNCTION",
            example = "[\"COMPOSITE\",\"FUNCTION\"]")
    private List<String> actionTypes;
}
