package com.datamaster.module.ontology.controller.admin.action.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/** AI 动作决策结果。 */
@Schema(description = "AI 本体动作决策 Response VO")
@Data
public class AiActionDecisionRespVO {

    @Schema(description = "决策：ALLOW/ASK_HUMAN/REJECT", example = "ALLOW")
    private String decision;

    @Schema(description = "AI 选择的已存在动作ID")
    private Long actionId;

    @Schema(description = "动作名称")
    private String actionName;

    @Schema(description = "AI 补全后的执行参数 JSON")
    private String inputParams;

    @Schema(description = "决策理由")
    private String reason;

    @Schema(description = "AI 置信度，0 到 1")
    private Double confidence;

    @Schema(description = "是否必须人工补充选择或确认")
    private Boolean requiresHuman;

    @Schema(description = "缺少的参数或引用对象")
    private List<String> missingFields;

    @Schema(description = "动作提交前置条件求值结果")
    private String criteriaResult;

}
