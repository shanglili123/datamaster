package com.datamaster.module.assets.controller.admin.skill.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * Multi-table Skill generate request.
 */
@Schema(description = "Multi-table Skill generate request")
@Data
public class AiMultiTableSkillGenerateReqVO {

    @Schema(description = "数据源ID", example = "12")
    private Long datasourceId;

    @Schema(description = "表名列表")
    private List<String> tableNames;

    private Boolean publish;

    private Boolean forceRefresh;

    private String manualNotes;
}
