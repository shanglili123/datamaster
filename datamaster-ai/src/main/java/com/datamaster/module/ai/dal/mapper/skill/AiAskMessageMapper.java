package com.datamaster.module.ai.dal.mapper.skill;

import com.datamaster.module.ai.dal.dataobject.skill.AiAskMessageDO;
import com.datamaster.mybatis.core.mapper.BaseMapperX;
import com.datamaster.mybatis.core.query.LambdaQueryWrapperX;

import java.util.Collections;
import java.util.List;

/**
 * AI ask-data message mapper.
 */
public interface AiAskMessageMapper extends BaseMapperX<AiAskMessageDO> {

    default List<AiAskMessageDO> selectLatest(Long sessionId, Integer limit) {
        List<AiAskMessageDO> rows = selectList(new LambdaQueryWrapperX<AiAskMessageDO>()
                .eq(AiAskMessageDO::getSessionId, sessionId)
                .orderByDesc(AiAskMessageDO::getId)
                .last("LIMIT " + normalizeLimit(limit, 10)));
        Collections.reverse(rows);
        return rows;
    }

    default List<AiAskMessageDO> selectBefore(Long sessionId, Long beforeId, Integer limit) {
        List<AiAskMessageDO> rows = selectList(new LambdaQueryWrapperX<AiAskMessageDO>()
                .eq(AiAskMessageDO::getSessionId, sessionId)
                .lt(beforeId != null, AiAskMessageDO::getId, beforeId)
                .orderByDesc(AiAskMessageDO::getId)
                .last("LIMIT " + normalizeLimit(limit, 5)));
        Collections.reverse(rows);
        return rows;
    }

    default List<AiAskMessageDO> selectAfter(Long sessionId, Long afterId, Integer limit) {
        return selectList(new LambdaQueryWrapperX<AiAskMessageDO>()
                .eq(AiAskMessageDO::getSessionId, sessionId)
                .gt(afterId != null, AiAskMessageDO::getId, afterId)
                .orderByAsc(AiAskMessageDO::getId)
                .last("LIMIT " + normalizeLimit(limit, 5)));
    }

    default Long countBySessionId(Long sessionId) {
        return selectCount(new LambdaQueryWrapperX<AiAskMessageDO>()
                .eq(AiAskMessageDO::getSessionId, sessionId));
    }

    default int deleteBySessionId(Long sessionId) {
        return delete(new LambdaQueryWrapperX<AiAskMessageDO>()
                .eq(AiAskMessageDO::getSessionId, sessionId));
    }

    static int normalizeLimit(Integer limit, int defaultLimit) {
        return limit == null ? defaultLimit : Math.max(1, Math.min(limit, 50));
    }
}

