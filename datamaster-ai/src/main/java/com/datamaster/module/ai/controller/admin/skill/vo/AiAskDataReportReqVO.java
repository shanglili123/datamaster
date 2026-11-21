package com.datamaster.module.ai.controller.admin.skill.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.util.List;
import java.util.Map;

/**
 * Ask-data report generation request.
 */
@Schema(description = "决策智能体报告生成请求")
@Data
public class AiAskDataReportReqVO {

    @Schema(description = "用户报告需求", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "用户问题不能为空")
    private String question;

    @Schema(description = "业务本体ID；用于解析报告所需的绑定数据源")
    private Long ontologyId;

    @Schema(description = "数据源ID")
    private Long datasourceId;

    @Schema(description = "空间ID")
    private Long spaceId;

    @Schema(description = "空间编码")
    private String spaceCode;

    @Schema(description = "Skill ID")
    private Long skillId;

    @Schema(description = "报告模板ID，不传时使用Skill默认模板")
    private Long templateId;

    @Schema(description = "当前空间模型配置ID；不传则使用空间路由")
    private Long modelProfileId;

    @Schema(description = "旧独立配置模式模型名；接口同步模式不可使用")
    private String model;

    @Schema(description = "是否要求返回SQL用于校验")
    private Boolean returnSql;

    @Schema(description = "报告参数，例如周期、区域")
    private Map<String, Object> params;

    @Schema(description = "DataMaster 会话ID，用于保持报告对话上下文", example = "12345")
    private String conversationId;

    @Schema(description = "最近几轮会话上下文")
    private List<Map<String, String>> history;
}

