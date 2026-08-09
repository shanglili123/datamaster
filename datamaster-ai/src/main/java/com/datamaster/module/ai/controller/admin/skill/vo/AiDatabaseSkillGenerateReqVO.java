package com.datamaster.module.ai.controller.admin.skill.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * Database Skill generate request.
 */
@Schema(description = "Database Skill generate request")
@Data
public class AiDatabaseSkillGenerateReqVO {

    @Schema(description = "数据源ID", example = "12")
    private Long datasourceId;

    private Boolean publish;

    private Boolean forceRefresh;

    private String manualNotes;
}

