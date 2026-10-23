package com.datamaster.ingestion.dal.mapper;

import com.datamaster.ingestion.dal.dataobject.IngestionLogDO;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.datamaster.mybatis.core.mapper.BaseMapperX;

import java.util.Date;
import java.util.List;

/**
 * 数据接收处理日志 Mapper
 */
public interface IngestionLogMapper extends BaseMapperX<IngestionLogDO> {

    /**
     * 查询已经完成业务处理的 Kafka 事件。失败记录不能作为幂等成功依据，
     * 否则消息修复后将无法再次消费。
     */
    default IngestionLogDO selectProcessed(String topic, Integer partition, Long offset) {
        if (topic == null || partition == null || offset == null) {
            return null;
        }
        return selectOne(new QueryWrapper<IngestionLogDO>()
                .eq("topic", topic)
                .eq("partition", partition)
                .eq("msg_offset", offset)
                .in("status", "SUCCESS", "CONFLICT_SKIP")
                .orderByDesc("id")
                .last("LIMIT 1"));
    }

    default List<IngestionLogDO> selectDeadLetters(Integer limit) {
        return selectDeadLetters(limit, null, null);
    }

    default List<IngestionLogDO> selectDeadLetters(Integer limit, String topic, String targetTable) {
        QueryWrapper<IngestionLogDO> wrapper = new QueryWrapper<IngestionLogDO>()
                .eq("status", "DEAD_LETTER")
                .orderByDesc("id");
        if (topic != null && !topic.trim().isEmpty()) {
            wrapper.eq("topic", topic.trim());
        }
        if (targetTable != null && !targetTable.trim().isEmpty()) {
            String table = targetTable.trim();
            wrapper.and(w -> w.eq("target_table", table)
                    .or().like("target_table", table + ",%")
                    .or().like("target_table", "%," + table)
                    .or().like("target_table", "%," + table + ",%"));
        }
        if (limit != null && limit > 0) {
            wrapper.last("LIMIT " + Math.min(limit, 200));
        }
        return selectList(wrapper);
    }

    /** 原子领取一条死信，避免多个管理员同时重放同一条消息。 */
    default boolean tryMarkReplaying(Long logId) {
        if (logId == null) {
            return false;
        }
        return update(null, new UpdateWrapper<IngestionLogDO>()
                .eq("id", logId)
                .apply("(status = 'DEAD_LETTER' OR (status = 'REPLAYING' "
                        + "AND COALESCE(update_time, create_time) < CURRENT_TIMESTAMP - INTERVAL '10 minutes'))")
                .set("status", "REPLAYING")
                .set("update_time", new Date())
                .set("message", "死信重放处理中")) > 0;
    }

    default void updateReplayStatus(Long logId, String status, String message) {
        if (logId == null) {
            return;
        }
        update(null, new UpdateWrapper<IngestionLogDO>()
                .eq("id", logId)
                .eq("status", "REPLAYING")
                .set("status", status)
                .set("message", message)
                .set("update_time", new Date()));
    }

    default long countByStatus(String status) {
        if (status == null || status.trim().isEmpty()) {
            return 0L;
        }
        return selectCount(new QueryWrapper<IngestionLogDO>().eq("status", status));
    }
}
