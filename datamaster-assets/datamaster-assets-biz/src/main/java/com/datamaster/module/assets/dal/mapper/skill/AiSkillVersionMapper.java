package com.datamaster.module.assets.dal.mapper.skill;

import com.datamaster.module.assets.dal.dataobject.skill.AiSkillVersionDO;
import com.datamaster.mybatis.core.mapper.BaseMapperX;
import com.datamaster.mybatis.core.query.LambdaQueryWrapperX;

import java.util.List;

/**
 * AI Skill version mapper.
 */
public interface AiSkillVersionMapper extends BaseMapperX<AiSkillVersionDO> {

    default List<AiSkillVersionDO> selectListBySkillId(Long skillId) {
        return selectList(new LambdaQueryWrapperX<AiSkillVersionDO>()
                .eq(AiSkillVersionDO::getSkillId, skillId)
                .orderByDesc(AiSkillVersionDO::getVersion));
    }

    default AiSkillVersionDO selectBySkillIdAndVersion(Long skillId, Integer version) {
        return selectOne(new LambdaQueryWrapperX<AiSkillVersionDO>()
                .eq(AiSkillVersionDO::getSkillId, skillId)
                .eq(AiSkillVersionDO::getVersion, version));
    }
}
