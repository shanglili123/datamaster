package com.datamaster.module.system.dal.mapper;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

public interface SysHomeMapper {

    @Select("SELECT COUNT(*) FROM COL_ETL_TASK WHERE SPACE_ID = #{spaceId} AND TYPE = '1' AND DEL_FLAG = '0'")
    long countIntegrationTaskTotal(@Param("spaceId") Long spaceId);

    @Select("SELECT COUNT(DISTINCT t.ID) FROM COL_ETL_TASK t INNER JOIN COL_ETL_TASK_INSTANCE i ON t.ID = i.TASK_ID WHERE t.SPACE_ID = #{spaceId} AND t.TYPE = '1' AND t.DEL_FLAG = '0' AND i.STATUS IN ('6', '8') AND i.DEL_FLAG = '0'")
    long countIntegrationTaskFailed(@Param("spaceId") Long spaceId);

    @Select("SELECT COUNT(*) FROM COL_ETL_TASK WHERE SPACE_ID = #{spaceId} AND TYPE = '3' AND DEL_FLAG = '0'")
    long countDevelopTaskTotal(@Param("spaceId") Long spaceId);

    @Select("SELECT COUNT(DISTINCT t.ID) FROM COL_ETL_TASK t INNER JOIN COL_ETL_TASK_INSTANCE i ON t.ID = i.TASK_ID WHERE t.SPACE_ID = #{spaceId} AND t.TYPE = '3' AND t.DEL_FLAG = '0' AND i.STATUS IN ('6', '8') AND i.DEL_FLAG = '0'")
    long countDevelopTaskFailed(@Param("spaceId") Long spaceId);

    @Select("SELECT COUNT(*) FROM SVC_API_LOG WHERE SPACE_ID = #{spaceId} AND DEL_FLAG = '0'")
    long countApiCallTotal(@Param("spaceId") Long spaceId);

    @Select("SELECT COUNT(*) FROM SVC_API_LOG WHERE SPACE_ID = #{spaceId} AND STATUS = '0' AND DEL_FLAG = '0'")
    long countApiCallFailed(@Param("spaceId") Long spaceId);

    @Select("SELECT ds.DATASOURCE_NAME AS datasourceName, ct.SCHEMA_NAME AS schemaName, ct.TABLE_NAME AS tableName, ct.ROW_COUNT AS rowCount, ct.CREATE_TIME AS updateTime FROM CAT_TABLE ct INNER JOIN AST_DATASOURCE_SPACE_REL rel ON ct.DATASOURCE_ID = rel.DATASOURCE_ID LEFT JOIN AST_DATASOURCE ds ON ct.DATASOURCE_ID = ds.ID WHERE rel.SPACE_ID = #{spaceId} AND ct.DEL_FLAG = '0' AND rel.VALID_FLAG = '1' AND ds.DEL_FLAG = '0' ORDER BY ct.ROW_COUNT DESC LIMIT 20")
    List<Map<String, Object>> selectTableRows(@Param("spaceId") Long spaceId);

    @Select("SELECT COUNT(*) FROM AST_DATASOURCE_SPACE_REL WHERE SPACE_ID = #{spaceId} AND VALID_FLAG = '1'")
    long countDatasourceTotal(@Param("spaceId") Long spaceId);

    @Select("SELECT COUNT(*) FROM CAT_TABLE t INNER JOIN AST_DATASOURCE_SPACE_REL rel ON t.DATASOURCE_ID = rel.DATASOURCE_ID WHERE rel.SPACE_ID = #{spaceId} AND t.DEL_FLAG = '0' AND rel.VALID_FLAG = '1'")
    long countCatalogTableTotal(@Param("spaceId") Long spaceId);

    @Select("SELECT COUNT(*) FROM SVC_API WHERE SPACE_ID = #{spaceId} AND DEL_FLAG = '0'")
    long countApiTotal(@Param("spaceId") Long spaceId);

    @Select("SELECT COUNT(*) FROM STD_DATA_ELEM WHERE SPACE_ID = #{spaceId} AND DEL_FLAG = '0'")
    long countDataElemTotal(@Param("spaceId") Long spaceId);

    @Select("SELECT COUNT(*) FROM STD_MODEL WHERE SPACE_ID = #{spaceId} AND DEL_FLAG = '0'")
    long countModelTotal(@Param("spaceId") Long spaceId);

    @Select("SELECT COUNT(*) FROM TAX_TAG WHERE SPACE_ID = #{spaceId} AND DEL_FLAG = '0'")
    long countTagTotal(@Param("spaceId") Long spaceId);

    @Select("SELECT COUNT(*) FROM CAT_TASK WHERE SPACE_ID = #{spaceId} AND DEL_FLAG = '0'")
    long countCollectTaskTotal(@Param("spaceId") Long spaceId);

    @Select("SELECT COUNT(*) FROM STD_DOCUMENT WHERE SPACE_ID = #{spaceId} AND DEL_FLAG = '0'")
    long countDocumentTotal(@Param("spaceId") Long spaceId);
}
