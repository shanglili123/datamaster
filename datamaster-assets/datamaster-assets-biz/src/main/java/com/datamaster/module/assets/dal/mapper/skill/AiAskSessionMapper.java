package com.datamaster.module.assets.dal.mapper.skill;

import com.datamaster.module.assets.dal.dataobject.skill.AiAskSessionDO;
import com.datamaster.mybatis.core.mapper.BaseMapperX;
import com.datamaster.mybatis.core.query.LambdaQueryWrapperX;

import java.util.List;

/**
 * AI ask-data session mapper.
 */
public interface AiAskSessionMapper extends BaseMapperX<AiAskSessionDO> {

    default List<AiAskSessionDO> selectRecent(Long userId, Long projectId, Integer limit) {
        return selectList(new LambdaQueryWrapperX<AiAskSessionDO>()
                .eq(AiAskSessionDO::getUserId, userId)
                .eq(projectId != null, AiAskSessionDO::getProjectId, projectId)
                .orderByDesc(AiAskSessionDO::getUpdateTime)
                .orderByDesc(AiAskSessionDO::getId)
                .last("LIMIT " + (limit == null ? 10 : Math.max(1, Math.min(limit, 50)))));
    }
}
