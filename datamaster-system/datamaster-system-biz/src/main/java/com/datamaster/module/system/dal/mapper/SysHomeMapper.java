package com.datamaster.module.system.dal.mapper;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

public interface SysHomeMapper {

    @Select("SELECT COUNT(*) FROM COL_ETL_TASK WHERE PROJECT_ID = #{projectId} AND TYPE = '1' AND DEL_FLAG = '0'")
    long countIntegrationTaskTotal(@Param("projectId") Long projectId);

    @Select("SELECT COUNT(DISTINCT t.ID) FROM COL_ETL_TASK t INNER JOIN COL_ETL_TASK_INSTANCE i ON t.ID = i.TASK_ID WHERE t.PROJECT_ID = #{projectId} AND t.TYPE = '1' AND t.DEL_FLAG = '0' AND i.STATUS IN ('6', '8') AND i.DEL_FLAG = '0'")
    long countIntegrationTaskFailed(@Param("projectId") Long projectId);

    @Select("SELECT COUNT(*) FROM COL_ETL_TASK WHERE PROJECT_ID = #{projectId} AND TYPE = '3' AND DEL_FLAG = '0'")
    long countDevelopTaskTotal(@Param("projectId") Long projectId);

    @Select("SELECT COUNT(DISTINCT t.ID) FROM COL_ETL_TASK t INNER JOIN COL_ETL_TASK_INSTANCE i ON t.ID = i.TASK_ID WHERE t.PROJECT_ID = #{projectId} AND t.TYPE = '3' AND t.DEL_FLAG = '0' AND i.STATUS IN ('6', '8') AND i.DEL_FLAG = '0'")
    long countDevelopTaskFailed(@Param("projectId") Long projectId);

    @Select("SELECT COUNT(*) FROM SVC_API_LOG WHERE PROJECT_ID = #{projectId} AND DEL_FLAG = '0'")
    long countApiCallTotal(@Param("projectId") Long projectId);

    @Select("SELECT COUNT(*) FROM SVC_API_LOG WHERE PROJECT_ID = #{projectId} AND STATUS = '0' AND DEL_FLAG = '0'")
    long countApiCallFailed(@Param("projectId") Long projectId);

    @Select("SELECT ds.DATASOURCE_NAME AS datasourceName, ct.SCHEMA_NAME AS schemaName, ct.TABLE_NAME AS tableName, ct.ROW_COUNT AS rowCount, ct.CREATE_TIME AS updateTime FROM CAT_TABLE ct INNER JOIN AST_DATASOURCE_PROJECT_REL rel ON ct.DATASOURCE_ID = rel.DATASOURCE_ID LEFT JOIN AST_DATASOURCE ds ON ct.DATASOURCE_ID = ds.ID WHERE rel.PROJECT_ID = #{projectId} AND ct.DEL_FLAG = '0' AND rel.VALID_FLAG = '1' AND ds.DEL_FLAG = '0' ORDER BY ct.ROW_COUNT DESC LIMIT 20")
    List<Map<String, Object>> selectTableRows(@Param("projectId") Long projectId);

    @Select("SELECT COUNT(*) FROM AST_DATASOURCE_PROJECT_REL WHERE PROJECT_ID = #{projectId} AND VALID_FLAG = '1'")
    long countDatasourceTotal(@Param("projectId") Long projectId);

    @Select("SELECT COUNT(*) FROM CAT_TABLE t INNER JOIN AST_DATASOURCE_PROJECT_REL rel ON t.DATASOURCE_ID = rel.DATASOURCE_ID WHERE rel.PROJECT_ID = #{projectId} AND t.DEL_FLAG = '0' AND rel.VALID_FLAG = '1'")
    long countCatalogTableTotal(@Param("projectId") Long projectId);

    @Select("SELECT COUNT(*) FROM SVC_API WHERE PROJECT_ID = #{projectId} AND DEL_FLAG = '0'")
    long countApiTotal(@Param("projectId") Long projectId);

    @Select("SELECT COUNT(*) FROM STD_DATA_ELEM WHERE PROJECT_ID = #{projectId} AND DEL_FLAG = '0'")
    long countDataElemTotal(@Param("projectId") Long projectId);

    @Select("SELECT COUNT(*) FROM STD_MODEL WHERE PROJECT_ID = #{projectId} AND DEL_FLAG = '0'")
    long countModelTotal(@Param("projectId") Long projectId);

    @Select("SELECT COUNT(*) FROM TAX_TAG WHERE PROJECT_ID = #{projectId} AND DEL_FLAG = '0'")
    long countTagTotal(@Param("projectId") Long projectId);

    @Select("SELECT COUNT(*) FROM CAT_TASK WHERE PROJECT_ID = #{projectId} AND DEL_FLAG = '0'")
    long countCollectTaskTotal(@Param("projectId") Long projectId);

    @Select("SELECT COUNT(*) FROM STD_DOCUMENT WHERE PROJECT_ID = #{projectId} AND DEL_FLAG = '0'")
    long countDocumentTotal(@Param("projectId") Long projectId);
}
