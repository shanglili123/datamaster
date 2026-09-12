package com.datamaster.module.ai.dal.mapper.skill;

import com.datamaster.common.core.page.PageResult;
import com.datamaster.common.utils.StringUtils;
import com.datamaster.module.ai.controller.admin.skill.vo.AiSkillPageReqVO;
import com.datamaster.module.ai.dal.dataobject.skill.AiSkillDO;
import com.datamaster.mybatis.core.mapper.BaseMapperX;
import com.datamaster.mybatis.core.query.LambdaQueryWrapperX;

import java.util.List;

/**
 * AI Skill mapper.
 */
public interface AiSkillMapper extends BaseMapperX<AiSkillDO> {

    default PageResult<AiSkillDO> selectPage(AiSkillPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<AiSkillDO>()
                .eq(reqVO.getId() != null, AiSkillDO::getId, reqVO.getId())
                .like(StringUtils.isNotBlank(reqVO.getSkillCode()), AiSkillDO::getSkillCode, reqVO.getSkillCode())
                .like(StringUtils.isNotBlank(reqVO.getSkillName()), AiSkillDO::getSkillName, reqVO.getSkillName())
                .eq(StringUtils.isNotBlank(reqVO.getSkillType()), AiSkillDO::getSkillType, reqVO.getSkillType())
                .eq(StringUtils.isNotBlank(reqVO.getStatus()), AiSkillDO::getStatus, reqVO.getStatus())
                .eq(StringUtils.isNotBlank(reqVO.getSourceType()), AiSkillDO::getSourceType, reqVO.getSourceType())
                .eq(StringUtils.isNotBlank(reqVO.getBizObjectType()), AiSkillDO::getBizObjectType, reqVO.getBizObjectType())
                .eq(reqVO.getBizObjectId() != null, AiSkillDO::getBizObjectId, reqVO.getBizObjectId())
                .orderByDesc(AiSkillDO::getUpdateTime));
    }

    default AiSkillDO selectBySkillCode(String skillCode) {
        return selectOne(new LambdaQueryWrapperX<AiSkillDO>()
                .eq(AiSkillDO::getSkillCode, skillCode));
    }

    default AiSkillDO selectByBizObject(String bizObjectType, Long bizObjectId) {
        return selectOne(new LambdaQueryWrapperX<AiSkillDO>()
                .eq(AiSkillDO::getBizObjectType, bizObjectType)
                .eq(AiSkillDO::getBizObjectId, bizObjectId));
    }

    default List<AiSkillDO> selectPublishedByKeyword(String keyword) {
        LambdaQueryWrapperX<AiSkillDO> wrapper = new LambdaQueryWrapperX<AiSkillDO>()
                .eq(AiSkillDO::getStatus, "PUBLISHED")
                .in(AiSkillDO::getSkillType, java.util.Arrays.asList("TABLE", "DATABASE", "MULTI_TABLE", "ONTOLOGY_DECISION", "REPORT_TEMPLATE"))
                .orderByDesc(AiSkillDO::getUpdateTime);
        if (StringUtils.isNotBlank(keyword)) {
            wrapper.and(item -> item
                    .like(AiSkillDO::getSkillCode, keyword)
                    .or()
                    .like(AiSkillDO::getSkillName, keyword)
                    .or()
                    .like(AiSkillDO::getContent, keyword));
        }
        return selectList(wrapper);
    }

    default List<AiSkillDO> selectPublishedPlatformSkills() {
        return selectList(new LambdaQueryWrapperX<AiSkillDO>()
                .eq(AiSkillDO::getStatus, "PUBLISHED")
                .in(AiSkillDO::getSkillType, java.util.Arrays.asList("PLATFORM_METADATA", "PLATFORM_QUALITY"))
                .orderByDesc(AiSkillDO::getUpdateTime));
    }

    default List<AiSkillDO> selectPublishedSkills() {
        return selectList(new LambdaQueryWrapperX<AiSkillDO>()
                .eq(AiSkillDO::getStatus, "PUBLISHED")
                .orderByDesc(AiSkillDO::getUpdateTime));
    }

    default List<AiSkillDO> selectPublishedTableSkills() {
        return selectList(new LambdaQueryWrapperX<AiSkillDO>()
                .eq(AiSkillDO::getStatus, "PUBLISHED")
                .eq(AiSkillDO::getSkillType, "TABLE")
                .orderByDesc(AiSkillDO::getUpdateTime));
    }
}

