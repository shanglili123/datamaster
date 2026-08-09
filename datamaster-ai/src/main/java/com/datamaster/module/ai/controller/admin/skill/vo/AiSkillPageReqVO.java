package com.datamaster.module.ai.controller.admin.skill.vo;

import com.datamaster.common.core.page.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * AI Skill page request.
 */
@Schema(description = "AI Skill page request")
@Data
public class AiSkillPageReqVO extends PageParam {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String skillCode;

    private String skillName;

    private String skillType;

    private String status;

    private String sourceType;

    private String bizObjectType;

    private Long bizObjectId;
}

