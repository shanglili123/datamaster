package com.datamaster.module.ai.controller.admin.skill.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * Ask-data SQL generation response.
 */
@Schema(description = "决策智能体查询响应")
@Data
public class AiAskDataSqlRespVO {

    @Schema(description = "原始问题")
    private String question;

    @Schema(description = "生成的SQL")
    private String sql;

    @Schema(description = "SQL解释")
    private String explanation;

    @Schema(description = "引用的Skill列表")
    private List<AiSkillRespVO> referencedSkills;

    @Schema(description = "质量风险提示")
    private String qualityWarning;

}

