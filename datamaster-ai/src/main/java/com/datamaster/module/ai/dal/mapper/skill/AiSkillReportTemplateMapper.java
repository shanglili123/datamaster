package com.datamaster.module.ai.dal.mapper.skill;

import com.datamaster.module.ai.dal.dataobject.skill.AiSkillReportTemplateDO;
import com.datamaster.mybatis.core.mapper.BaseMapperX;
import com.datamaster.mybatis.core.query.LambdaQueryWrapperX;

import java.util.List;

/**
 * AI Skill report template mapper.
 */
public interface AiSkillReportTemplateMapper extends BaseMapperX<AiSkillReportTemplateDO> {

    default List<AiSkillReportTemplateDO> selectListBySkillId(Long skillId) {
        return selectList(new LambdaQueryWrapperX<AiSkillReportTemplateDO>()
                .eq(AiSkillReportTemplateDO::getSkillId, skillId)
                .orderByDesc(AiSkillReportTemplateDO::getDefaultFlag)
                .orderByDesc(AiSkillReportTemplateDO::getUpdateTime));
    }

    default AiSkillReportTemplateDO selectBySkillIdAndCode(Long skillId, String templateCode) {
        return selectOne(new LambdaQueryWrapperX<AiSkillReportTemplateDO>()
                .eq(AiSkillReportTemplateDO::getSkillId, skillId)
                .eq(AiSkillReportTemplateDO::getTemplateCode, templateCode));
    }

    default AiSkillReportTemplateDO selectDefaultBySkillId(Long skillId) {
        return selectOne(new LambdaQueryWrapperX<AiSkillReportTemplateDO>()
                .eq(AiSkillReportTemplateDO::getSkillId, skillId)
                .eq(AiSkillReportTemplateDO::getDefaultFlag, Boolean.TRUE));
    }
}

