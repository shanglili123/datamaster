package com.datamaster.module.assets.controller.admin.skill.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * Table Skill generate request.
 */
@Schema(description = "Table Skill generate request")
@Data
public class AiTableSkillGenerateReqVO {

    @Schema(description = "元数据表ID；兼容旧字段名 assetId", example = "1001")
    private Long assetId;

    @Schema(description = "数据源ID；未传元数据表ID时必填", example = "12")
    private Long datasourceId;

    @Schema(description = "表名；未传元数据表ID时必填", example = "ods_order")
    private String tableName;

    private Boolean publish;

    private Boolean forceRefresh;

    private String manualNotes;
}
