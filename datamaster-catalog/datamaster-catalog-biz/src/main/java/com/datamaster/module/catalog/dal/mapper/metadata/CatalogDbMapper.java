package com.datamaster.module.catalog.dal.mapper.metadata;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.google.common.collect.Sets;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.catalog.controller.admin.metadata.vo.CatalogDbPageReqVO;
import com.datamaster.module.catalog.controller.admin.metadata.vo.CatalogMetaSearchRespDTO;
import com.datamaster.module.catalog.dal.dataobject.metadata.CatalogDbDO;
import com.datamaster.mybatis.core.mapper.BaseMapperX;
import com.datamaster.mybatis.core.query.BizDataScopeQueryHelper;
import com.datamaster.mybatis.core.query.MPJLambdaWrapperX;

import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * 数据库Mapper接口
 *
 * @author DATAMASTER
 * @date 2026-02-11
 */
public interface CatalogDbMapper extends BaseMapperX<CatalogDbDO> {

    default PageResult<CatalogDbDO> selectPage(CatalogDbPageReqVO reqVO) {
        if ("0".equals(reqVO.getSourceSystemName())) {
            reqVO.setSourceSystemName(null);
        }
        // 定义排序的字段（防止 SQL 注入，与数据库字段名称一致）
        boolean selfScopeWithUnassigned = BizDataScopeQueryHelper.useSelfScopeWithUnassigned(
                reqVO.getBizScopeMode(), reqVO.getBizScopeIncludeUnassigned(), reqVO.getBusinessLeader());
        boolean deptScopeWithUnassigned = BizDataScopeQueryHelper.useDeptScopeWithUnassigned(
                reqVO.getBizScopeMode(), reqVO.getBizScopeIncludeUnassigned(), reqVO.getResponsibleDept());
        Set<String> allowedColumns = Sets.newHashSet("id", "create_time", "update_time", "audit_time",
                "table_count", "data_quality");

        MPJLambdaWrapperX<CatalogDbDO> lambdaWrapperX = new MPJLambdaWrapperX<>();
        lambdaWrapperX.selectAll(CatalogDbDO.class)
                .select("(SELECT COUNT(*) FROM Catalog_TABLE o WHERE o.DB_ID = t.ID) table_count");
        lambdaWrapperX.eqIfPresent(CatalogDbDO::getTaskId, reqVO.getTaskId())
                .eqIfPresent(CatalogDbDO::getSourceSystemId, reqVO.getSourceSystemId())
                .eqIfPresent(CatalogDbDO::getVersion, reqVO.getVersion())
                .eqIfPresent(CatalogDbDO::getDatasourceId, reqVO.getDatasourceId())
                .eqIfPresent(CatalogDbDO::getIp, reqVO.getIp())
                .eqIfPresent(CatalogDbDO::getPort, reqVO.getPort())
                .eqIfPresent(CatalogDbDO::getDatasourceConfig, reqVO.getDatasourceConfig())
                .eqIfPresent(CatalogDbDO::getDbType, reqVO.getDbType())
                .likeIfPresent(CatalogDbDO::getDbName, reqVO.getDbName())
                .likeIfPresent(CatalogDbDO::getSchemaName, reqVO.getSchemaName())
                .eqIfPresent(CatalogDbDO::getSafetyLevelId, reqVO.getSafetyLevelId())
                .eqIfPresent(CatalogDbDO::getBelongingLayer, reqVO.getBelongingLayer())
                .eqIfPresent(CatalogDbDO::getBelongingSystem, reqVO.getBelongingSystem())
                .eqIfPresent(CatalogDbDO::getBusinessLeaderPhone, reqVO.getBusinessLeaderPhone())
                .eqIfPresent(CatalogDbDO::getTechLeader, reqVO.getTechLeader())
                .eqIfPresent(CatalogDbDO::getTechLeaderPhone, reqVO.getTechLeaderPhone())
                .eqIfPresent(CatalogDbDO::getStorageSize, reqVO.getStorageSize())
                .eqIfPresent(CatalogDbDO::getDataQuality, reqVO.getDataQuality())
                .eqIfPresent(CatalogDbDO::getAuditStatus, reqVO.getAuditStatus())
                .eqIfPresent(CatalogDbDO::getAuditTime, reqVO.getAuditTime())
                .eqIfPresent(CatalogDbDO::getStatus, reqVO.getStatus())
                .eqIfPresent(CatalogDbDO::getCreateTime, reqVO.getCreateTime())
                .eqIfPresent(CatalogDbDO::getDescription, reqVO.getDescription())
                .eq(reqVO.getProjectId() != null, CatalogDbDO::getProjectId, reqVO.getProjectId())
                .orderBy(reqVO.getOrderByColumn(), reqVO.getIsAsc(), allowedColumns);
        lambdaWrapperX.apply(selfScopeWithUnassigned, "(t.BUSINESS_LEADER = {0} OR (t.BUSINESS_LEADER IS NULL AND t.RESPONSIBLE_DEPT IS NULL))", reqVO.getBusinessLeader());
        lambdaWrapperX.eq(!selfScopeWithUnassigned && reqVO.getBusinessLeader() != null, CatalogDbDO::getBusinessLeader, reqVO.getBusinessLeader());
        lambdaWrapperX.apply(deptScopeWithUnassigned, "(t.RESPONSIBLE_DEPT = {0} OR (t.BUSINESS_LEADER IS NULL AND t.RESPONSIBLE_DEPT IS NULL))", reqVO.getResponsibleDept());
        lambdaWrapperX.eq(!deptScopeWithUnassigned && reqVO.getResponsibleDept() != null, CatalogDbDO::getResponsibleDept, reqVO.getResponsibleDept());
        lambdaWrapperX.likeRightIfExists(CatalogDbDO::getSourceSystemName, reqVO.getSourceSystemName());
        // 构造动态查询条件
        return selectPage(reqVO, lambdaWrapperX);
    }

    default CatalogDbDO findById(Long id) {
        MPJLambdaWrapperX<CatalogDbDO> lambdaWrapperX = new MPJLambdaWrapperX<>();
        lambdaWrapperX.selectAll(CatalogDbDO.class)
                .select("(SELECT COUNT(*) FROM Catalog_TABLE o WHERE o.DB_ID = t.ID ) tableCount")
                .select("(SELECT COUNT(*) FROM Catalog_COLUMN c WHERE c.DB_ID = t.ID ) columnCount")
                .select("(SELECT u.NICK_NAME FROM SYSTEM_USER u WHERE u.USER_ID = t.BUSINESS_LEADER) businessLeaderName")
                .select("(SELECT n.NICK_NAME FROM SYSTEM_USER n WHERE n.USER_ID = t.TECH_LEADER) techLeaderName")
//                .select("(SELECT s.SENSITIVE_LEVEL FROM DG_SENSITIVE_LEVEL s WHERE s.ID = t.SAFETY_LEVEL_ID) safetyLevelName")
                .select("(SELECT d.DEPT_NAME FROM SYSTEM_DEPT d WHERE d.DEPT_ID = t.RESPONSIBLE_DEPT) responsibleDeptName")
                .select("t2.NAME AS sourceSystemName")
                .leftJoin("TAX_SOURCE_SYSTEM t2 on t.SOURCE_SYSTEM_ID = t2.ID AND t2.DEL_FLAG = '0'")
                .eq(CatalogDbDO::getId, id);
        return selectOne(lambdaWrapperX);
    }


    default boolean existsBySourceSystemName(String sourceSystemName) {
        return exists(Wrappers.lambdaQuery(CatalogDbDO.class)
                .likeRight(CatalogDbDO::getSourceSystemName, sourceSystemName));
    }


    /**
     * 元数据检索分页查询
     */
    List<CatalogMetaSearchRespDTO> selectMetaSearchPage(
            @Param("keyword") String keyword,
            @Param("types") List<String> types,     // DB-1 / TABLE-2 / COLUMN-3
            @Param("dbTypes") List<String> dbTypes, // mysql / oracle / dm / kingbase8
            @Param("startTime") Date startTime,
            @Param("endTime") Date endTime,
            @Param("offset") Integer offset,
            @Param("pageSize") Integer pageSize
    );

    /**
     * 元数据检索总数
     */
    Long selectMetaSearchCount(
            @Param("keyword") String keyword,
            @Param("types") List<String> types,
            @Param("dbTypes") List<String> dbTypes,
            @Param("startTime") Date startTime,
            @Param("endTime") Date endTime
    );


    /**
     * 更新数据库存储大小
     * @param dbId 数据库ID
     * @return 更新结果
     */
	@Update("UPDATE Catalog_DB a SET a.STORAGE_SIZE = (SELECT SUM(storage_size) FROM Catalog_TABLE b WHERE b.db_id = a.id AND b.del_flag = '0') WHERE a.id = #{dbId} AND a.del_flag = '0'")
	int updateStorageSizeById(@Param("dbId") Long dbId);

    /**
     * 更新元数据表的字段数
     * @param dbId 数据库ID
     * @return 更新结果
     */
	@Update("UPDATE Catalog_TABLE a SET a.column_count = (SELECT COUNT(*) FROM Catalog_COLUMN b WHERE b.table_id = a.id AND a.db_id = b.db_id and b.del_flag='0') WHERE EXISTS (SELECT 1 FROM Catalog_COLUMN b WHERE b.table_id = a.id AND a.db_id = b.db_id and b.del_flag='0') AND a.db_id = #{dbId} and a.del_flag='0'")
	int updateColumnCountByDbId(@Param("dbId") Long dbId);

    /**
     * 更新数据库数据行数
     * @param dbId 数据库ID
     * @return 更新结果
     */
	@Update("UPDATE Catalog_DB a SET a.data_row_count = (SELECT SUM(row_count) FROM Catalog_TABLE b WHERE b.db_id = a.id AND b.del_flag = '0') WHERE a.id = #{dbId} AND a.del_flag = '0'")
	int updateDataRowCountById(@Param("dbId") Long dbId);

}
