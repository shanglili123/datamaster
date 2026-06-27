package com.datamaster.module.assets.service.skill;

import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.assets.controller.admin.skill.vo.AiSkillPageReqVO;
import com.datamaster.module.assets.controller.admin.skill.vo.AiSkillRespVO;
import com.datamaster.module.assets.controller.admin.skill.vo.AiSkillSaveReqVO;
import com.datamaster.module.assets.controller.admin.skill.vo.AiSkillVersionRespVO;
import com.datamaster.module.assets.controller.admin.skill.vo.AiTableSkillGenerateReqVO;

import java.util.List;

/**
 * AI Skill service.
 */
public interface IAiSkillService {

    PageResult<AiSkillRespVO> getSkillPage(AiSkillPageReqVO pageReqVO);

    AiSkillRespVO getSkill(Long id);

    Long createSkill(AiSkillSaveReqVO saveReqVO);

    Integer updateSkill(AiSkillSaveReqVO saveReqVO);

    Integer removeSkill(Long id);

    Integer publishSkill(Long id);

    Integer rollbackSkill(Long id, Integer version);

    List<AiSkillVersionRespVO> getSkillVersions(Long id);

    AiSkillRespVO generateMetadataSkill(Boolean publish);

    AiSkillRespVO generateQualitySkill(Boolean publish);

    AiSkillRespVO generateTableSkill(AiTableSkillGenerateReqVO reqVO);
}
