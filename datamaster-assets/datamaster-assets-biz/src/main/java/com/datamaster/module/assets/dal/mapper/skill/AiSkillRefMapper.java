package com.datamaster.module.assets.dal.mapper.skill;

import com.datamaster.module.assets.dal.dataobject.skill.AiSkillRefDO;
import com.datamaster.mybatis.core.mapper.BaseMapperX;
import com.datamaster.mybatis.core.query.LambdaQueryWrapperX;

/**
 * AI Skill reference mapper.
 */
public interface AiSkillRefMapper extends BaseMapperX<AiSkillRefDO> {

    default void deleteBySkillId(Long skillId) {
        delete(new LambdaQueryWrapperX<AiSkillRefDO>()
                .eq(AiSkillRefDO::getSkillId, skillId));
    }
}
