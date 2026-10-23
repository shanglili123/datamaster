package com.datamaster.metadata.dal.mapper.lineage;

import com.datamaster.metadata.dal.dataobject.lineage.LineageRows.*;
import org.apache.ibatis.annotations.*;
import java.util.Collection;
import java.util.List;

@Mapper
public interface LineageMapper {
    @Insert("INSERT INTO cat_lineage_producer(id,task_id,space_id,space_code,sequence,active,deleted,status) " +
            "VALUES(#{id},#{taskId},#{spaceId},#{spaceCode},-1,false,false,'EMPTY') ON CONFLICT(id) DO NOTHING")
    int ensureProducer(ProducerRow producer);

    @Select("SELECT * FROM cat_lineage_producer WHERE id=#{id} FOR UPDATE")
    ProducerRow lockProducer(String id);

    @Update("UPDATE cat_lineage_producer SET task_name=#{taskName},task_type=#{taskType},task_code=#{taskCode}," +
            "sequence=#{sequence},version=#{version},status=#{status},error_code=#{errorCode},object_ids_json=#{objectIdsJson},active=#{active}," +
            "deleted=#{deleted},stale=#{stale},lastgood_sequence=#{lastgoodSequence},update_time=CURRENT_TIMESTAMP WHERE id=#{id}")
    int updateProducer(ProducerRow producer);

    @Insert("INSERT INTO cat_lineage_version(producer_id,sequence,version,event_type,status,error_code) " +
            "VALUES(#{p.id},#{p.sequence},#{p.version},#{eventType},#{p.status},#{p.errorCode})")
    int insertVersion(@Param("p") ProducerRow producer, @Param("eventType") String eventType);

    @Insert("INSERT INTO cat_lineage_object(id,kind,datasource_id,catalog_name,schema_name,object_name,external_key,column_name,parent_id) " +
            "VALUES(#{id},#{kind},#{datasourceId},#{catalogName},#{schemaName},#{objectName},#{externalKey},#{columnName},#{parentId}) " +
            "ON CONFLICT(id) DO NOTHING")
    int insertObject(ObjectRow object);

    @Delete("DELETE FROM cat_lineage_edge WHERE producer_id=#{id}")
    int clearEdges(String id);

    @Insert("INSERT INTO cat_lineage_edge(id,producer_id,sequence,source_id,target_id,source_column_id,target_column_id,unit_key,route_key,kind,confidence) " +
            "VALUES(#{id},#{producerId},#{sequence},#{sourceId},#{targetId},#{sourceColumnId},#{targetColumnId},#{unitKey},#{routeKey},#{kind},#{confidence}) " +
            "ON CONFLICT(id) DO NOTHING")
    int insertEdge(EdgeRow edge);

    @Select("SELECT * FROM cat_lineage_object WHERE id=#{id}")
    ObjectRow object(String id);

    @Select({"<script>SELECT * FROM cat_lineage_object WHERE id IN",
            "<foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach></script>"})
    @Options(timeout = 2)
    List<ObjectRow> objects(@Param("ids") Collection<String> ids);

    @Select({"<script>SELECT e.*,p.task_id,p.task_name,p.status,p.error_code,p.active,",
            "p.stale FROM cat_lineage_edge e",
            "JOIN cat_lineage_producer p ON p.id=e.producer_id",
            "WHERE p.space_id=#{spaceId} AND p.space_code=#{spaceCode} AND NOT p.deleted",
            "<if test='!includeInactive'>AND p.active</if>",
            "AND (<if test='direction != &quot;UPSTREAM&quot;'>e.source_id IN",
            "<foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach></if>",
            "<if test='direction == &quot;BOTH&quot;'> OR </if>",
            "<if test='direction != &quot;DOWNSTREAM&quot;'>e.target_id IN",
            "<foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach></if>)",
            "ORDER BY e.id LIMIT #{limit}</script>"})
    @Options(timeout = 2)
    List<EdgeRow> adjacent(@Param("ids") Collection<String> ids, @Param("spaceId") Long spaceId,
                          @Param("spaceCode") String spaceCode, @Param("direction") String direction,
                          @Param("includeInactive") boolean includeInactive, @Param("limit") int limit);

    @Select({"<script>SELECT status,error_code,stale FROM cat_lineage_producer p WHERE p.space_id=#{spaceId}",
            "AND p.space_code=#{spaceCode} AND NOT p.deleted <if test='!includeInactive'>AND p.active</if>",
            "AND jsonb_exists_any(CAST(p.object_ids_json AS jsonb), ARRAY[",
            "<foreach collection='ids' item='id' separator=','>CAST(#{id} AS text)</foreach>])",
            "ORDER BY p.id LIMIT 200</script>"})
    @Options(timeout = 2)
    List<ProducerRow> diagnostics(@Param("ids") Collection<String> ids, @Param("spaceId") Long spaceId,
                                   @Param("spaceCode") String spaceCode, @Param("includeInactive") boolean includeInactive);

    @Select("SELECT * FROM cat_lineage_object WHERE kind='TABLE' AND datasource_id=#{datasourceId} " +
            "AND catalog_name IS NOT DISTINCT FROM #{catalogName} AND schema_name IS NOT DISTINCT FROM #{schemaName} " +
            "AND object_name=#{objectName} ORDER BY id LIMIT 2")
    @Options(timeout = 2)
    List<ObjectRow> resolve(@Param("datasourceId") Long datasourceId, @Param("catalogName") String catalog,
                            @Param("schemaName") String schema, @Param("objectName") String table);
}
