package com.datamaster.module.ai.service.skill;

public interface IAiModelGatewayService {

    String complete(String systemPrompt, String userPrompt);

    boolean available();

    default com.datamaster.module.ai.service.skill.dto.ReportTemplateOutput generateReportTemplate(String systemPrompt, String userPrompt) {
        throw new UnsupportedOperationException("当前模型网关不支持类型化报告模板");
    }

    default com.datamaster.module.ai.service.skill.dto.SkillEnhancementOutput enhanceSkill(String systemPrompt, String userPrompt) {
        throw new UnsupportedOperationException("当前模型网关不支持类型化Skill增强");
    }
}

