package com.datamaster.module.collector.dal.mapper.etl;

import com.datamaster.module.collector.dal.dataobject.etl.CollectorEtlTaskOpsEventDO;
import com.datamaster.mybatis.core.mapper.BaseMapperX;

public interface CollectorEtlTaskOpsEventMapper extends BaseMapperX<CollectorEtlTaskOpsEventDO> {
    @org.apache.ibatis.annotations.Select("SELECT * FROM col_etl_task_ops_event WHERE action='RESTART_COMPONENT_TASK' " +
            "AND action_status NOT IN ('SUCCESS','EXHAUSTED','MANUAL','CANCELLED') AND del_flag=FALSE AND id > #{afterId} ORDER BY id LIMIT 100")
    java.util.List<CollectorEtlTaskOpsEventDO> pendingComponent(@org.apache.ibatis.annotations.Param("afterId") Long afterId);

    @org.apache.ibatis.annotations.Select("SELECT * FROM col_etl_task_ops_event WHERE action='RESTART_COMPONENT_TASK' " +
            "AND action_status IN ('SUCCESS','EXHAUSTED','MANUAL') AND ai_raw_result LIKE '%\"notificationPending\":true%' " +
            "AND del_flag=FALSE AND id > #{afterId} ORDER BY id LIMIT 100")
    java.util.List<CollectorEtlTaskOpsEventDO> pendingComponentNotices(@org.apache.ibatis.annotations.Param("afterId") Long afterId);

    @org.apache.ibatis.annotations.Select("SELECT * FROM col_etl_task_ops_event WHERE task_id=#{taskId} " +
            "AND action='RESTART_COMPONENT_TASK' AND del_flag=FALSE ORDER BY id DESC LIMIT 1")
    CollectorEtlTaskOpsEventDO latestComponent(@org.apache.ibatis.annotations.Param("taskId") Long taskId);

    @org.apache.ibatis.annotations.Select("SELECT * FROM col_etl_task_ops_event WHERE id=#{id} AND del_flag=FALSE FOR UPDATE")
    CollectorEtlTaskOpsEventDO lockEvent(@org.apache.ibatis.annotations.Param("id") Long id);

    @org.apache.ibatis.annotations.Update("UPDATE col_etl_task_ops_event SET action_status=#{event.actionStatus}, " +
            "ai_raw_result=#{event.aiRawResult}, reason=#{event.reason}, suggestion=#{event.suggestion}, " +
            "failure_type=#{event.failureType}, risk_level=#{event.riskLevel}, recoverable=#{event.recoverable}, " +
            "log_excerpt=#{event.logExcerpt}, update_time=CURRENT_TIMESTAMP " +
            "WHERE id=#{event.id} AND action='RESTART_COMPONENT_TASK' AND action_status=#{expected}")
    int transition(@org.apache.ibatis.annotations.Param("event") CollectorEtlTaskOpsEventDO event,
                   @org.apache.ibatis.annotations.Param("expected") String expected);
}
