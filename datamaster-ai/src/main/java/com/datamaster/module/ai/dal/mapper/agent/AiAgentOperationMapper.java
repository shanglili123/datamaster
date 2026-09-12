package com.datamaster.module.ai.dal.mapper.agent;

import com.datamaster.module.ai.dal.dataobject.agent.AiAgentOperationDO;
import com.datamaster.mybatis.core.mapper.BaseMapperX;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;

import java.util.List;

/** 数据智能体运营流程实例 Mapper。 */
public interface AiAgentOperationMapper extends BaseMapperX<AiAgentOperationDO> {

    default List<AiAgentOperationDO> selectLatest(Long spaceId, String spaceCode, int limit) {
        QueryWrapper<AiAgentOperationDO> wrapper = new QueryWrapper<>();
        wrapper.eq(spaceId != null, "space_id", spaceId)
                .eq(spaceCode != null && !spaceCode.trim().isEmpty(), "space_code", spaceCode)
                .eq("del_flag", false)
                .orderByDesc("update_time")
                .last("LIMIT " + Math.max(1, Math.min(limit, 20)));
        return selectList(wrapper);
    }

    default List<AiAgentOperationDO> selectRunning() {
        return selectList(new QueryWrapper<AiAgentOperationDO>()
                .eq("del_flag", false)
                .eq("status", "RUNNING"));
    }

    default AiAgentOperationDO selectActiveByDatabaseName(String databaseName) {
        if (databaseName == null || databaseName.trim().isEmpty()) return null;
        return selectOne(new QueryWrapper<AiAgentOperationDO>()
                .apply("LOWER(database_name) = LOWER({0})", databaseName.trim())
                .eq("del_flag", false)
                .orderByDesc("update_time")
                .last("LIMIT 1"));
    }
}
