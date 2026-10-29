package com.datamaster.module.collector.dal.mapper.etl;

import com.datamaster.module.collector.dal.dataobject.etl.CollectorEtlRunDO;
import org.apache.ibatis.annotations.*;
import java.util.List;

/** 新组件运行登记复用现有流程实例表；当前owner由任务行保证唯一。 */
@Mapper
public interface CollectorEtlRunMapper {
    String COLUMNS = "i.id, i.task_id, i.ds_id AS workflow_instance_id, i.task_code AS workflow_code, " +
            "i.run_owner_token AS owner_token, i.run_guard_status AS status, i.run_lease_updated_at AS lease_updated_at, " +
            "i.create_time, i.update_time";

    @Select("SELECT id FROM col_etl_task WHERE id=#{taskId} AND del_flag='0' FOR UPDATE")
    Long lockTask(@Param("taskId") Long taskId);

    @Update("UPDATE col_etl_task_instance SET run_owner_token=#{run.ownerToken}, run_guard_status='ACTIVE', " +
            "run_lease_updated_at=CURRENT_TIMESTAMP WHERE task_id=#{run.taskId} AND ds_id=#{run.workflowInstanceId} " +
            "AND run_owner_token IS NULL AND del_flag='0'")
    int insertIfAbsent(@Param("run") CollectorEtlRunDO run);

    @Select("SELECT " + COLUMNS + " FROM col_etl_task_instance i WHERE i.task_id=#{taskId} AND i.ds_id=#{workflowInstanceId} " +
            "AND i.run_owner_token IS NOT NULL AND i.del_flag='0'")
    CollectorEtlRunDO getByWorkflowInstance(@Param("taskId") Long taskId, @Param("workflowInstanceId") Long workflowInstanceId);

    @Select("SELECT " + COLUMNS + " FROM col_etl_task_instance i WHERE i.id=#{runId} AND i.del_flag='0' FOR UPDATE")
    CollectorEtlRunDO lockById(@Param("runId") Long runId);

    @Select("SELECT " + COLUMNS + " FROM col_etl_task_instance i WHERE i.run_guard_status IN ('ACTIVE','UNCERTAIN','PAUSED') " +
            "AND i.del_flag='0' AND i.id > #{afterId} ORDER BY i.id LIMIT 200")
    List<CollectorEtlRunDO> findUnfinished(@Param("afterId") Long afterId);

    @Select("SELECT " + COLUMNS + " FROM col_etl_task_instance i WHERE i.task_id=#{taskId} " +
            "AND i.run_guard_status IN ('ACTIVE','UNCERTAIN','PAUSED') AND i.del_flag='0' ORDER BY i.id")
    List<CollectorEtlRunDO> findActive(@Param("taskId") Long taskId);

    @Update("UPDATE col_etl_task SET active_run_id=#{run.id}, run_workflow_instance_id=#{run.workflowInstanceId}, " +
            "run_workflow_code=#{run.workflowCode}, run_owner_token=#{run.ownerToken}, run_guard_status='ACTIVE', run_resources_json='[]'," +
            "run_lease_updated_at=CURRENT_TIMESTAMP WHERE id=#{run.taskId} AND del_flag='0' " +
            "AND (active_run_id IS NULL OR (active_run_id=#{run.id} AND run_owner_token=#{run.ownerToken}))")
    int acquireTask(@Param("run") CollectorEtlRunDO run);

    @Select("SELECT active_run_id FROM col_etl_task WHERE id=#{taskId}")
    Long taskOwner(@Param("taskId") Long taskId);

    @Update("UPDATE col_etl_task SET active_run_id=NULL, run_workflow_instance_id=NULL, run_workflow_code=NULL, " +
            "run_owner_token=NULL, run_guard_status=NULL, run_resources_json=NULL, run_lease_updated_at=NULL " +
            "WHERE id=#{taskId} AND active_run_id=#{runId} AND run_owner_token=#{ownerToken}")
    int releaseTask(@Param("taskId") Long taskId, @Param("runId") Long runId, @Param("ownerToken") String ownerToken);

    @Update("UPDATE col_etl_task SET run_guard_status=#{status}, run_lease_updated_at=CURRENT_TIMESTAMP " +
            "WHERE id=#{taskId} AND active_run_id=#{runId} AND run_owner_token=#{ownerToken}")
    int mirrorTask(@Param("taskId") Long taskId, @Param("runId") Long runId,
                   @Param("ownerToken") String ownerToken, @Param("status") String status);

    @Update("UPDATE col_etl_task_instance SET run_lease_updated_at=CURRENT_TIMESTAMP WHERE id=#{runId} " +
            "AND run_owner_token=#{ownerToken} AND run_guard_status IN ('ACTIVE','UNCERTAIN','PAUSED')")
    int heartbeat(@Param("runId") Long runId, @Param("ownerToken") String ownerToken);

    @Update("UPDATE col_etl_task_instance SET run_guard_status=#{newStatus} WHERE id=#{runId} AND run_owner_token=#{ownerToken} " +
            "AND run_guard_status=#{expectedStatus} AND run_guard_status IN ('ACTIVE','UNCERTAIN','PAUSED') " +
            "AND #{newStatus} IN ('ACTIVE','UNCERTAIN','PAUSED')")
    int compareAndSetActiveStatus(@Param("runId") Long runId, @Param("ownerToken") String ownerToken,
                                 @Param("expectedStatus") String expectedStatus, @Param("newStatus") String newStatus);

    @Update("UPDATE col_etl_task_instance SET run_guard_status=#{terminalStatus} WHERE id=#{runId} AND run_owner_token=#{ownerToken} " +
            "AND run_guard_status=#{expectedStatus} AND run_guard_status IN ('ACTIVE','UNCERTAIN','PAUSED') " +
            "AND #{terminalStatus} IN ('SUCCESS','FAILURE','STOP')")
    int compareAndSetTerminal(@Param("runId") Long runId, @Param("ownerToken") String ownerToken,
                             @Param("expectedStatus") String expectedStatus, @Param("terminalStatus") String terminalStatus);
}
