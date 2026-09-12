package com.datamaster.module.ai.service.skill;

import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.ai.controller.admin.skill.vo.AiSkillPageReqVO;
import com.datamaster.module.ai.controller.admin.skill.vo.AiSkillRespVO;
import com.datamaster.module.ai.controller.admin.skill.vo.AiSkillSaveReqVO;
import com.datamaster.module.ai.controller.admin.skill.vo.AiSkillVersionRespVO;
import com.datamaster.module.ai.controller.admin.skill.vo.AiDatabaseSkillGenerateReqVO;
import com.datamaster.module.ai.controller.admin.skill.vo.AiMultiTableSkillGenerateReqVO;
import com.datamaster.module.ai.controller.admin.skill.vo.AiOntologySkillGenerateReqVO;
import com.datamaster.module.ai.controller.admin.skill.vo.AiTableSkillGenerateReqVO;
import com.datamaster.module.ai.controller.admin.skill.vo.AiSkillReportTemplateGenerateReqVO;

import java.util.List;

/**
 * AI Skill service.
 */
public interface IAiSkillService {

    PageResult<AiSkillRespVO> getSkillPage(AiSkillPageReqVO pageReqVO);

    AiSkillRespVO getSkill(Long id);

    /** 根据自然语言需求生成报告模板 JSON。 */
    String generateReportTemplate(Long skillId, AiSkillReportTemplateGenerateReqVO reqVO);

    Long createSkill(AiSkillSaveReqVO saveReqVO);

    Integer updateSkill(AiSkillSaveReqVO saveReqVO);

    Integer removeSkill(Long id);

    Integer publishSkill(Long id);

    Integer rollbackSkill(Long id, Integer version);

    List<AiSkillVersionRespVO> getSkillVersions(Long id);

    AiSkillRespVO generateMetadataSkill(Boolean publish);

    AiSkillRespVO generateQualitySkill(Boolean publish);

    AiSkillRespVO generateTableSkill(AiTableSkillGenerateReqVO reqVO);

    AiSkillRespVO generateDatabaseSkill(AiDatabaseSkillGenerateReqVO reqVO);

    AiSkillRespVO generateMultiTableSkill(AiMultiTableSkillGenerateReqVO reqVO);

    AiSkillRespVO generateOntologyDecisionSkill(AiOntologySkillGenerateReqVO reqVO);
}

