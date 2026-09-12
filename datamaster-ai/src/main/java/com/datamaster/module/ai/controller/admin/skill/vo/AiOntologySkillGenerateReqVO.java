package com.datamaster.module.ai.controller.admin.skill.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/** 本体决策 Skill 生成请求。 */
@Schema(description = "本体决策 Skill 生成请求")
@Data
public class AiOntologySkillGenerateReqVO {

    @Schema(description = "本体ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long ontologyId;

    private Boolean publish;

    private String manualNotes;
}
