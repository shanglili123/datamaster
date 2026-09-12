package com.datamaster.module.ai.controller.admin.skill.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * AI report template generation request.
 */
@Schema(description = "AI报告模板生成请求")
@Data
public class AiSkillReportTemplateGenerateReqVO {

    @NotBlank(message = "模板需求不能为空")
    @Schema(description = "模板需求描述", required = true)
    private String prompt;
}
