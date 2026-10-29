package com.datamaster.module.collector.dal.mapper.etl;

import com.datamaster.module.collector.dal.dataobject.etl.CollectorEtlNodeInstanceDO;
import org.apache.ibatis.annotations.*;
import java.util.List;

/** Fixed stage window, replay status and successful watermark live on the existing stage instance. */
@Mapper
public interface CollectorEtlStageRecordMapper {
    @Select("SELECT id FROM col_etl_task WHERE id=#{taskId} AND del_flag='0' FOR UPDATE")
    Long lockTask(@Param("taskId") Long taskId);

    @Select("SELECT n.* FROM col_etl_node_instance n JOIN col_etl_task_instance i ON i.id=n.task_instance_id " +
            "WHERE i.task_id=#{taskId} AND i.id=#{instanceId} AND n.stable_stage_key=#{stage} " +
            "AND i.del_flag='0' AND n.del_flag='0'")
    CollectorEtlNodeInstanceDO current(@Param("taskId") Long taskId, @Param("stage") String stage, @Param("instanceId") Long instanceId);

    @Select("SELECT n.* FROM col_etl_node_instance n JOIN col_etl_task_instance i ON i.id=n.task_instance_id " +
            "WHERE i.task_id=#{taskId} AND n.stable_stage_key=#{stage} AND n.incremental_window_status IS NOT NULL " +
            "AND i.del_flag='0' AND n.del_flag='0' ORDER BY n.id DESC LIMIT 1 FOR UPDATE")
    CollectorEtlNodeInstanceDO latest(@Param("taskId") Long taskId, @Param("stage") String stage);

    @Select("SELECT n.* FROM col_etl_node_instance n JOIN col_etl_task_instance i ON i.id=n.task_instance_id " +
            "WHERE i.task_id=#{taskId} AND i.id=#{instanceId} AND n.incremental_window_status IS NOT NULL " +
            "AND i.del_flag='0' AND n.del_flag='0' ORDER BY n.stable_stage_key")
    List<CollectorEtlNodeInstanceDO> list(@Param("taskId") Long taskId, @Param("instanceId") Long instanceId);

    @Insert("INSERT INTO col_etl_node_instance (id,task_instance_id,task_instance_name,task_type,name,node_type,node_id,node_code,node_version," +
            "space_id,space_code,parameters,status,ds_task_instance_id,stable_stage_key,incremental_config_hash,incremental_start_value,incremental_end_value," +
            "incremental_window_empty,incremental_window_status,incremental_replay_from_id,incremental_sources_json,valid_flag,del_flag,create_time,update_time)" +
            "VALUES (#{id},#{taskInstanceId},#{taskInstanceName},#{taskType},#{name},#{nodeType},#{nodeId},#{nodeCode},#{nodeVersion}," +
            "#{spaceId},#{spaceCode},#{parameters},#{status},#{dsTaskInstanceId},#{stableStageKey},#{incrementalConfigHash},#{incrementalStartValue},#{incrementalEndValue}," +
            "#{incrementalWindowEmpty},#{incrementalWindowStatus},#{incrementalReplayFromId},#{incrementalSourcesJson},'1','0',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)")
    int insertWindow(CollectorEtlNodeInstanceDO record);

    @Update("UPDATE col_etl_node_instance SET stable_stage_key=#{stableStageKey}, incremental_config_hash=#{incrementalConfigHash}, " +
            "incremental_start_value=#{incrementalStartValue}, incremental_end_value=#{incrementalEndValue}, " +
            "incremental_window_empty=#{incrementalWindowEmpty}, incremental_window_status='PREPARED', " +
            "incremental_replay_from_id=#{incrementalReplayFromId} WHERE id=#{id} AND incremental_window_status IS NULL")
    int attachWindow(CollectorEtlNodeInstanceDO record);

    @Update("UPDATE col_etl_node_instance SET node_code=#{code}, node_version=#{version}, incremental_sources_json=#{sources} " +
            "WHERE id=#{id} AND incremental_window_status='PREPARED'")
    int describe(@Param("id") Long id, @Param("code") String code, @Param("version") Integer version, @Param("sources") String sources);

    @Update("UPDATE col_etl_node_instance SET incremental_window_status=#{status}, " +
            "incremental_successful_value=#{successful,jdbcType=VARCHAR} WHERE id=#{id} AND incremental_window_status='PREPARED'")
    int finish(@Param("id") Long id, @Param("status") String status, @Param("successful") String successful);
}
