package com.datamaster.module.ai.dal.mapper.skill;

import com.datamaster.module.ai.dal.dataobject.skill.AiSkillRefDO;
import com.datamaster.mybatis.core.mapper.BaseMapperX;
import com.datamaster.mybatis.core.query.LambdaQueryWrapperX;
import java.util.List;
import java.util.stream.Collectors;

/**
 * AI Skill reference mapper.
 */
public interface AiSkillRefMapper extends BaseMapperX<AiSkillRefDO> {

    default void deleteBySkillId(Long skillId) {
        delete(new LambdaQueryWrapperX<AiSkillRefDO>()
                .eq(AiSkillRefDO::getSkillId, skillId));
    }

    default void deleteBySkillIdAndRefType(Long skillId, String refType) {
        delete(new LambdaQueryWrapperX<AiSkillRefDO>()
                .eq(AiSkillRefDO::getSkillId, skillId)
                .eq(AiSkillRefDO::getRefType, refType));
    }

    default List<Long> selectSkillIdsByDatasourceId(Long datasourceId) {
        return selectList(new LambdaQueryWrapperX<AiSkillRefDO>()
                .eq(AiSkillRefDO::getRefType, "DATA_SOURCE")
                .eq(AiSkillRefDO::getRefId, datasourceId))
                .stream().map(AiSkillRefDO::getSkillId).distinct().collect(Collectors.toList());
    }
}

