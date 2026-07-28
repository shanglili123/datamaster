package com.datamaster.module.assets.controller.admin.skill.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.util.Map;

/**
 * Ask-data report generation request.
 */
@Schema(description = "问数报告生成请求")
@Data
public class AiAskDataReportReqVO {

    @Schema(description = "用户报告需求", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "用户问题不能为空")
    private String question;

    @Schema(description = "数据源ID")
    private Long datasourceId;

    @Schema(description = "空间ID")
    private Long projectId;

    @Schema(description = "空间编码")
    private String projectCode;

    @Schema(description = "Skill ID")
    private Long skillId;

    @Schema(description = "报告模板ID，不传时使用Skill默认模板")
    private Long templateId;

    @Schema(description = "模型名称")
    private String model;

    @Schema(description = "是否要求返回SQL用于校验")
    private Boolean returnSql;

    @Schema(description = "报告参数，例如周期、区域")
    private Map<String, Object> params;
}
