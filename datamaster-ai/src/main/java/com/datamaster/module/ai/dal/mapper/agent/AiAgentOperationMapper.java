package com.datamaster.module.ai.dal.mapper.agent;

import com.datamaster.module.ai.dal.dataobject.agent.AiAgentOperationDO;
import com.datamaster.mybatis.core.mapper.BaseMapperX;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;

import java.util.List;

/** 数据智能体运营流程实例 Mapper。 */
public interface AiAgentOperationMapper extends BaseMapperX<AiAgentOperationDO> {

    default List<AiAgentOperationDO> selectLatest(Long spaceId, String spaceCode, Long creatorId, int limit) {
        QueryWrapper<AiAgentOperationDO> wrapper = new QueryWrapper<>();
        wrapper.eq(spaceId != null, "space_id", spaceId)
                .eq(spaceCode != null && !spaceCode.trim().isEmpty(), "space_code", spaceCode)
                .eq(creatorId != null, "creator_id", creatorId)
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

    default AiAgentOperationDO selectActiveByDatabaseName(String databaseName, Long creatorId,
                                                          Long spaceId, String spaceCode) {
        if (databaseName == null || databaseName.trim().isEmpty() || creatorId == null) return null;
        QueryWrapper<AiAgentOperationDO> wrapper = new QueryWrapper<AiAgentOperationDO>()
                .apply("LOWER(database_name) = LOWER({0})", databaseName.trim())
                .eq("creator_id", creatorId)
                .eq("del_flag", false);
        if (spaceId == null) {
            wrapper.isNull("space_id");
        } else {
            wrapper.eq("space_id", spaceId);
        }
        if (spaceCode == null || spaceCode.trim().isEmpty()) {
            wrapper.and(item -> item.isNull("space_code").or().eq("space_code", ""));
        } else {
            wrapper.eq("space_code", spaceCode.trim());
        }
        return selectOne(wrapper.orderByDesc("update_time").last("LIMIT 1"));
    }

    /**
     * 按稳定的“数据源 + 空间 + 操作者”流程标识查找已有流程。
     * 数据库名和目标描述都可能变化，不能再作为自主流程的唯一依据。
     */
    default AiAgentOperationDO selectByDatasourceAndSpace(Long datasourceId, Long creatorId,
                                                           Long spaceId, String spaceCode) {
        if (datasourceId == null || creatorId == null) return null;
        QueryWrapper<AiAgentOperationDO> wrapper = new QueryWrapper<AiAgentOperationDO>()
                .eq("datasource_id", datasourceId)
                .eq("creator_id", creatorId)
                .eq("del_flag", false);
        // 仅回收没有新流程键的历史记录；新流程必须按 flow_key 隔离。
        wrapper.and(item -> item.isNull("flow_key").or().eq("flow_key", ""));
        if (spaceId == null) wrapper.isNull("space_id"); else wrapper.eq("space_id", spaceId);
        if (spaceCode == null || spaceCode.trim().isEmpty()) {
            wrapper.and(item -> item.isNull("space_code").or().eq("space_code", ""));
        } else {
            wrapper.eq("space_code", spaceCode.trim());
        }
        return selectOne(wrapper.orderByDesc("update_time").last("LIMIT 1"));
    }

    default AiAgentOperationDO selectByFlowKey(String flowKey, Long creatorId) {
        if (flowKey == null || flowKey.trim().isEmpty() || creatorId == null) return null;
        return selectOne(new QueryWrapper<AiAgentOperationDO>()
                .eq("flow_key", flowKey.trim())
                .eq("creator_id", creatorId)
                .eq("del_flag", false)
                .orderByDesc("update_time")
                .last("LIMIT 1"));
    }
}
