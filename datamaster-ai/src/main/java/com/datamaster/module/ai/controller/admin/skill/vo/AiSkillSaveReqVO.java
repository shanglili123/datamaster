package com.datamaster.module.ai.controller.admin.skill.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * AI Skill save request.
 */
@Schema(description = "AI Skill save request")
@Data
public class AiSkillSaveReqVO {

    private Long id;

    @NotBlank(message = "Skill编码不能为空")
    private String skillCode;

    @NotBlank(message = "Skill名称不能为空")
    private String skillName;

    @NotBlank(message = "Skill类型不能为空")
    private String skillType;

    private String status;

    private String sourceType;

    private String bizObjectType;

    private Long bizObjectId;

    @NotBlank(message = "Skill内容不能为空")
    private String content;

    private String changeRemark;
}

