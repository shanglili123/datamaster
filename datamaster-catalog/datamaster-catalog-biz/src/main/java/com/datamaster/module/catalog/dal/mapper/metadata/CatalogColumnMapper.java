package com.datamaster.module.catalog.dal.mapper.metadata;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.google.common.collect.Sets;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.catalog.controller.admin.metadata.vo.CatalogColumnPageReqVO;
import com.datamaster.module.catalog.dal.dataobject.metadata.CatalogColumnDO;
import com.datamaster.module.catalog.dal.dataobject.metadata.CatalogDbDO;
import com.datamaster.module.catalog.dal.dataobject.metadata.CatalogTableDO;
import com.datamaster.mybatis.core.mapper.BaseMapperX;
import com.datamaster.mybatis.core.query.BizDataScopeQueryHelper;
import com.datamaster.mybatis.core.query.MPJLambdaWrapperX;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * 元数据字段信息Mapper接口
 *
 * @author DATAMASTER
 * @date 2026-02-11
 */
public interface CatalogColumnMapper extends BaseMapperX<CatalogColumnDO> {

    default PageResult<CatalogColumnDO> selectPage(CatalogColumnPageReqVO reqVO) {
        // 定义排序的字段（防止 SQL 注入，与数据库字段名称一致）
        boolean selfScopeWithUnassigned = BizDataScopeQueryHelper.useSelfScopeWithUnassigned(
                reqVO.getBizScopeMode(), reqVO.getBizScopeIncludeUnassigned(), reqVO.getBusinessLeader());
        boolean deptScopeWithUnassigned = BizDataScopeQueryHelper.useDeptScopeWithUnassigned(
                reqVO.getBizScopeMode(), reqVO.getBizScopeIncludeUnassigned(), reqVO.getResponsibleDept());
        Set<String> allowedColumns = Sets.newHashSet("id", "create_time", "update_time", "column_length",
                "column_precision", "column_scale", "audit_time", "data_quality");
        MPJLambdaWrapperX<CatalogColumnDO> lambdaWrapperX = new MPJLambdaWrapperX<>();
        lambdaWrapperX.selectAll(CatalogColumnDO.class)
                //.select("(SELECT e.NAME FROM DG_DATA_ELEM e WHERE e.ID = t.DATA_ELEM_ID) dataElemName")
                .select("t2.TABLE_NAME tableName")
                .select("t2.DB_NAME dbName")
                .select("d.source_system_id")
                .leftJoin("Catalog_TABLE t2 on t.TABLE_ID = t2.ID")
                .leftJoin("Catalog_DB d on t.DB_ID=d.id");

        lambdaWrapperX.eqIfPresent(CatalogColumnDO::getTaskId, reqVO.getTaskId())
                .eqIfPresent(CatalogColumnDO::getDbId, reqVO.getDbId())
                .eqIfPresent(CatalogColumnDO::getTableId, reqVO.getTableId())
                .eqIfPresent(CatalogColumnDO::getDatasourceId, reqVO.getDatasourceId())
                .eqIfPresent(CatalogColumnDO::getVersion, reqVO.getVersion())
                .eqIfPresent(CatalogColumnDO::getSafetyLevelId, reqVO.getSafetyLevelId())
                .eqIfPresent(CatalogColumnDO::getDataElemId, reqVO.getDataElemId())
                .likeIfPresent(CatalogColumnDO::getColumnName, reqVO.getColumnName())
                .likeIfPresent(CatalogColumnDO::getColumnComment, reqVO.getColumnComment())
                .eqIfPresent(CatalogColumnDO::getColumnType, reqVO.getColumnType())
                .eqIfPresent(CatalogColumnDO::getColumnLength, reqVO.getColumnLength())
                .eqIfPresent(CatalogColumnDO::getColumnPrecision, reqVO.getColumnPrecision())
                .eqIfPresent(CatalogColumnDO::getPortalVisible, reqVO.getPortalVisible())
                .eqIfPresent(CatalogColumnDO::getColumnScale, reqVO.getColumnScale())
                .eqIfPresent(CatalogColumnDO::getDefaultValue, reqVO.getDefaultValue())
                .eqIfPresent(CatalogColumnDO::getPkFlag, reqVO.getPkFlag())
                .eqIfPresent(CatalogColumnDO::getFkFlag, reqVO.getFkFlag())
                .eqIfPresent(CatalogColumnDO::getNullableFlag, reqVO.getNullableFlag())
                .eqIfPresent(CatalogColumnDO::getBusinessDefinition, reqVO.getBusinessDefinition())
                .eqIfPresent(CatalogColumnDO::getMeasuringUnit, reqVO.getMeasuringUnit())
                .eqIfPresent(CatalogColumnDO::getDataQuality, reqVO.getDataQuality())
                .eqIfPresent(CatalogColumnDO::getAuditStatus, reqVO.getAuditStatus())
                .eqIfPresent(CatalogColumnDO::getAuditTime, reqVO.getAuditTime())
                .eqIfPresent(CatalogColumnDO::getStatus, reqVO.getStatus())
                .eqIfPresent(CatalogColumnDO::getCreateTime, reqVO.getCreateTime())
                .eqIfPresent(CatalogColumnDO::getDescription, reqVO.getDescription())
                .orderBy(reqVO.getOrderByColumn(), reqVO.getIsAsc(), allowedColumns);
        lambdaWrapperX.apply(selfScopeWithUnassigned, "(t.BUSINESS_LEADER = {0} OR (t.BUSINESS_LEADER IS NULL AND t.RESPONSIBLE_DEPT IS NULL))", reqVO.getBusinessLeader());
        lambdaWrapperX.eq(!selfScopeWithUnassigned && reqVO.getBusinessLeader() != null, CatalogColumnDO::getBusinessLeader, reqVO.getBusinessLeader());
        lambdaWrapperX.apply(deptScopeWithUnassigned, "(t.RESPONSIBLE_DEPT = {0} OR (t.BUSINESS_LEADER IS NULL AND t.RESPONSIBLE_DEPT IS NULL))", reqVO.getResponsibleDept());
        lambdaWrapperX.eq(!deptScopeWithUnassigned && reqVO.getResponsibleDept() != null, CatalogColumnDO::getResponsibleDept, reqVO.getResponsibleDept());

        if (!"0".equals(reqVO.getSourceSystemName())) {
            lambdaWrapperX.likeRightIfExists("d", CatalogDbDO::getSourceSystemName, reqVO.getSourceSystemName());
        }
        // 构造动态查询条件
        return selectPage(reqVO, lambdaWrapperX);
    }

    default CatalogColumnDO findById(Long id) {
        MPJLambdaWrapperX<CatalogColumnDO> lambdaWrapperX = new MPJLambdaWrapperX<>();
        lambdaWrapperX.selectAll(CatalogColumnDO.class)
                //.select("(SELECT e.NAME FROM DG_DATA_ELEM e WHERE e.ID = t.DATA_ELEM_ID) dataElemName")
//                .select("(SELECT s.SENSITIVE_LEVEL FROM DG_SENSITIVE_LEVEL s WHERE s.ID = t.SAFETY_LEVEL_ID) safetyLevelName")
                .eq(CatalogTableDO::getId, id);
        return selectOne(lambdaWrapperX);
    }

    default boolean existsByDataElemIds(Collection<Long> dataElemIds) {
        return exists(Wrappers.lambdaQuery(CatalogColumnDO.class)
                .in(CatalogColumnDO::getDataElemId, dataElemIds));
    }

    default boolean existsByTableIds(Collection<Long> tableIds) {
        return exists(Wrappers.lambdaQuery(CatalogColumnDO.class)
                .in(CatalogColumnDO::getTableId, tableIds));
    }

    default boolean existsByTableId(Long tableId) {
        return exists(Wrappers.lambdaQuery(CatalogColumnDO.class)
                .eq(CatalogColumnDO::getTableId, tableId));
    }

    default List<CatalogColumnDO> findByTableIdAndColumnNameIn(Long tableId, Collection<String> columnNames) {
        return selectList(Wrappers.lambdaQuery(CatalogColumnDO.class)
                .eq(CatalogColumnDO::getTableId, tableId)
                .in(CatalogColumnDO::getColumnName, columnNames));
    }


}
