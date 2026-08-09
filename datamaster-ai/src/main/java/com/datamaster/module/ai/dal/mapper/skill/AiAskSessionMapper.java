package com.datamaster.module.ai.dal.mapper.skill;

import com.datamaster.module.ai.dal.dataobject.skill.AiAskSessionDO;
import com.datamaster.mybatis.core.mapper.BaseMapperX;
import com.datamaster.mybatis.core.query.LambdaQueryWrapperX;

import java.util.List;

/**
 * AI ask-data session mapper.
 */
public interface AiAskSessionMapper extends BaseMapperX<AiAskSessionDO> {

    default List<AiAskSessionDO> selectRecent(Long userId, Long spaceId, Integer limit) {
        return selectList(new LambdaQueryWrapperX<AiAskSessionDO>()
                .eq(AiAskSessionDO::getUserId, userId)
                .eq(spaceId != null, AiAskSessionDO::getSpaceId, spaceId)
                .orderByDesc(AiAskSessionDO::getUpdateTime)
                .orderByDesc(AiAskSessionDO::getId)
                .last("LIMIT " + (limit == null ? 10 : Math.max(1, Math.min(limit, 50)))));
    }
}

