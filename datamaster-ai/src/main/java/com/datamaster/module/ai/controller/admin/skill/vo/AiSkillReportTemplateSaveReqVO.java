package com.datamaster.module.ai.controller.admin.skill.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * AI Skill report template save request.
 */
@Schema(description = "AI Skill report template save request")
@Data
public class AiSkillReportTemplateSaveReqVO {

    private Long id;

    private Long skillId;

    @NotBlank(message = "模板编码不能为空")
    private String templateCode;

    @NotBlank(message = "模板名称不能为空")
    private String templateName;

    @NotBlank(message = "模板内容不能为空")
    private String templateContent;

    private String status;

    private Boolean defaultFlag;

    private String description;
}

