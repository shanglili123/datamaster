package com.datamaster.module.assets.service.skill;

import com.datamaster.module.assets.controller.admin.skill.vo.AiSkillReportTemplateRespVO;
import com.datamaster.module.assets.controller.admin.skill.vo.AiSkillReportTemplateSaveReqVO;

import java.util.List;

/**
 * AI Skill report template service.
 */
public interface IAiSkillReportTemplateService {

    List<AiSkillReportTemplateRespVO> listBySkillId(Long skillId);

    AiSkillReportTemplateRespVO getTemplate(Long skillId, Long templateId);

    Long createTemplate(Long skillId, AiSkillReportTemplateSaveReqVO reqVO);

    Integer updateTemplate(Long skillId, Long templateId, AiSkillReportTemplateSaveReqVO reqVO);

    Integer deleteTemplate(Long skillId, Long templateId);

    Integer setDefaultTemplate(Long skillId, Long templateId);
}
