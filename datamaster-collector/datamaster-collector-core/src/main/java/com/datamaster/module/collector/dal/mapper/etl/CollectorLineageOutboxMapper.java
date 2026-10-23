package com.datamaster.module.collector.dal.mapper.etl;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.datamaster.module.collector.dal.dataobject.etl.CollectorLineageOutboxDO;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface CollectorLineageOutboxMapper extends BaseMapper<CollectorLineageOutboxDO> {
    @Select("SELECT id FROM col_etl_task WHERE id=#{taskId} FOR UPDATE")
    Long lockTask(@Param("taskId") Long taskId);

    @Select("SELECT nextval('col_lineage_event_seq')")
    Long nextSequence();

    @Select("SELECT * FROM col_lineage_outbox WHERE status IN ('PENDING','RETRY','PROCESSING') " +
            "AND next_attempt_at <= CURRENT_TIMESTAMP AND (lease_until IS NULL OR lease_until < CURRENT_TIMESTAMP) " +
            "ORDER BY sequence LIMIT 20")
    List<CollectorLineageOutboxDO> due();

    @Update("UPDATE col_lineage_outbox SET status='PROCESSING', lease_token=#{token}, " +
            "lease_until=CURRENT_TIMESTAMP + INTERVAL '60 seconds' WHERE id=#{id} " +
            "AND status IN ('PENDING','RETRY','PROCESSING') AND (lease_until IS NULL OR lease_until < CURRENT_TIMESTAMP)")
    int claim(@Param("id") Long id, @Param("token") String token);

    @Update("UPDATE col_lineage_outbox SET status=#{status}, attempts=#{attempts}, error_code=#{error}, " +
            "next_attempt_at=#{next}, lease_until=NULL, lease_token=NULL WHERE id=#{id} AND lease_token=#{token}")
    int finish(@Param("id") Long id, @Param("token") String token, @Param("status") String status,
               @Param("attempts") int attempts, @Param("error") String error, @Param("next") java.util.Date next);

    @Select("SELECT * FROM col_lineage_outbox WHERE task_id=#{taskId} AND event_type='UPSERT' ORDER BY sequence DESC LIMIT 1")
    CollectorLineageOutboxDO latestSnapshot(@Param("taskId") Long taskId);

    @Select("SELECT COALESCE(MAX(sequence),0) FROM col_lineage_outbox WHERE task_id=#{taskId}")
    Long latestSequence(@Param("taskId") Long taskId);
}
