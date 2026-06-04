package com.datamaster.module.catalog.dal.mapper.metadata;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.google.common.collect.Sets;
import org.apache.commons.collections4.CollectionUtils;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.common.utils.StringUtils;
import com.datamaster.module.catalog.controller.admin.metadata.vo.CatalogTablePageReqVO;
import com.datamaster.module.catalog.dal.dataobject.metadata.CatalogDbDO;
import com.datamaster.module.catalog.dal.dataobject.metadata.CatalogTableDO;
import com.datamaster.mybatis.core.mapper.BaseMapperX;
import com.datamaster.mybatis.core.query.BizDataScopeQueryHelper;
import com.datamaster.mybatis.core.query.MPJLambdaWrapperX;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * 元数据信息Mapper接口
 *
 * @author DATAMASTER
 * @date 2026-02-11
 */
public interface CatalogTableMapper extends BaseMapperX<CatalogTableDO> {

    default PageResult<CatalogTableDO> selectPage(CatalogTablePageReqVO reqVO) {
        // 定义排序的字段（防止 SQL 注入，与数据库字段名称一致）
        boolean selfScopeWithUnassigned = BizDataScopeQueryHelper.useSelfScopeWithUnassigned(
                reqVO.getBizScopeMode(), reqVO.getBizScopeIncludeUnassigned(), reqVO.getBusinessLeader());
        boolean deptScopeWithUnassigned = BizDataScopeQueryHelper.useDeptScopeWithUnassigned(
                reqVO.getBizScopeMode(), reqVO.getBizScopeIncludeUnassigned(), reqVO.getResponsibleDept());
        Set<String> allowedColumns = Sets.newHashSet("id", "create_time", "update_time", "audit_time", "data_quality");
        MPJLambdaWrapperX<CatalogTableDO> lambdaWrapperX = new MPJLambdaWrapperX<>();
        lambdaWrapperX.selectAll(CatalogTableDO.class)
                .select("d.source_system_id",
                        "d.source_system_name",
                        "(CASE WHEN da.ID IS NULL THEN '0' ELSE '1' END) AS dssetFlag"
                        , "t4.DATASOURCE_NAME AS datasourceName"
                        , "t4.DATASOURCE_TYPE AS datasourceType"
                        , "u.PHONENUMBER AS createPhoneNumber"
                        , "u2.PHONENUMBER AS updatePhoneNumber"
                )
                .leftJoin("Catalog_DB d ON t.DB_ID=d.id")
                .leftJoin("SYSTEM_USER u on t.CREATOR_ID = u.USER_ID AND u.DEL_FLAG = '0'")
                .leftJoin("SYSTEM_USER u2 on t.UPDATER_ID = u2.USER_ID AND u2.DEL_FLAG = '0'")
                .leftJoin("AST_ASSET da ON da.TABLE_ID = t.ID AND da.DEL_FLAG = '0'")
                .leftJoin("AST_DATASOURCE t4 ON t.datasource_id = t4.id AND t4.DEL_FLAG = '0'");
        lambdaWrapperX.eqIfPresent(CatalogTableDO::getTaskId, reqVO.getTaskId())
                .eqIfPresent(CatalogTableDO::getDbId, reqVO.getDbId())
                .eqIfPresent(CatalogTableDO::getDatasourceId, reqVO.getDatasourceId())
                .eqIfPresent(CatalogTableDO::getVersion, reqVO.getVersion())
                .likeIfPresent(CatalogTableDO::getTableName, reqVO.getTableName())
                .likeIfPresent(CatalogTableDO::getTableComment, reqVO.getTableComment())
                .eqIfPresent(CatalogTableDO::getSafetyLevelId, reqVO.getSafetyLevelId())
                .likeIfPresent(CatalogTableDO::getDbName, reqVO.getDbName())
                .likeIfPresent(CatalogTableDO::getSchemaName, reqVO.getSchemaName())
                .eqIfPresent(CatalogTableDO::getStorageType, reqVO.getStorageType())
                .eqIfPresent(CatalogTableDO::getStorageSize, reqVO.getStorageSize())
                .eqIfPresent(CatalogTableDO::getBusinessLeaderPhone, reqVO.getBusinessLeaderPhone())
                .eqIfPresent(CatalogTableDO::getTechLeader, reqVO.getTechLeader())
                .eqIfPresent(CatalogTableDO::getTechLeaderPhone, reqVO.getTechLeaderPhone())
                .eqIfPresent(CatalogTableDO::getMasterFlag, reqVO.getMasterFlag())
                .eqIfPresent(CatalogTableDO::getTempFlag, reqVO.getTempFlag())
                .eqIfPresent(CatalogTableDO::getDataQuality, reqVO.getDataQuality())
                .eqIfPresent(CatalogTableDO::getAuditStatus, reqVO.getAuditStatus())
                .eqIfPresent(CatalogTableDO::getAuditTime, reqVO.getAuditTime())
                .eqIfPresent(CatalogTableDO::getStatus, reqVO.getStatus())
                .eqIfPresent(CatalogTableDO::getCreateTime, reqVO.getCreateTime())
                .eqIfPresent(CatalogTableDO::getDescription, reqVO.getDescription())
                .eq(reqVO.getProjectId() != null, CatalogTableDO::getProjectId, reqVO.getProjectId())
                .orderBy(reqVO.getOrderByColumn(), reqVO.getIsAsc(), allowedColumns);
        lambdaWrapperX.and(StringUtils.isNotBlank(reqVO.getKeyWord()), wrapper ->
                wrapper.like(CatalogTableDO::getTableName, reqVO.getKeyWord())
                        .or()
                        .like(CatalogTableDO::getTableComment, reqVO.getKeyWord()));
        lambdaWrapperX.notIn(StringUtils.isNotBlank(reqVO.getHideTableIds()),CatalogTableDO::getId, reqVO.getHideTableIds() != null ? Arrays.asList(reqVO.getHideTableIds().split(",")) : null);
        lambdaWrapperX.apply(selfScopeWithUnassigned, "(t.BUSINESS_LEADER = {0} OR (t.BUSINESS_LEADER IS NULL AND t.RESPONSIBLE_DEPT IS NULL))", reqVO.getBusinessLeader());
        lambdaWrapperX.eq(!selfScopeWithUnassigned && reqVO.getBusinessLeader() != null, CatalogTableDO::getBusinessLeader, reqVO.getBusinessLeader());
        lambdaWrapperX.apply(deptScopeWithUnassigned, "(t.RESPONSIBLE_DEPT = {0} OR (t.BUSINESS_LEADER IS NULL AND t.RESPONSIBLE_DEPT IS NULL))", reqVO.getResponsibleDept());
        lambdaWrapperX.eq(!deptScopeWithUnassigned && reqVO.getResponsibleDept() != null, CatalogTableDO::getResponsibleDept, reqVO.getResponsibleDept());
        if (!"0".equals(reqVO.getSourceSystemId())) {
            lambdaWrapperX.likeRightIfExists("d", CatalogDbDO::getSourceSystemId, reqVO.getSourceSystemId());
        }
        // 构造动态查询条件
        return selectPage(reqVO, lambdaWrapperX);
    }

    default PageResult<CatalogTableDO> getCatalogTablelist(CatalogTablePageReqVO reqVO){
        boolean selfScopeWithUnassigned = BizDataScopeQueryHelper.useSelfScopeWithUnassigned(
                reqVO.getBizScopeMode(), reqVO.getBizScopeIncludeUnassigned(), reqVO.getBusinessLeader());
        boolean deptScopeWithUnassigned = BizDataScopeQueryHelper.useDeptScopeWithUnassigned(
                reqVO.getBizScopeMode(), reqVO.getBizScopeIncludeUnassigned(), reqVO.getResponsibleDept());
        MPJLambdaWrapperX<CatalogTableDO> lambdaWrapperX = new MPJLambdaWrapperX<>();
        lambdaWrapperX.selectAll(CatalogTableDO.class)
                .select("t2.CRON_EXPRESSION as cronExpression")
                .leftJoin("Catalog_TASK_SCHEDULER t2 on t.TASK_ID= t2.TASK_ID and t2.DEL_FLAG = '0'");
        lambdaWrapperX
                .eq(reqVO.getTaskId() != null , CatalogTableDO::getTaskId, reqVO.getTaskId())
                .apply(selfScopeWithUnassigned, "(t.BUSINESS_LEADER = {0} OR (t.BUSINESS_LEADER IS NULL AND t.RESPONSIBLE_DEPT IS NULL))", reqVO.getBusinessLeader())
                .eq(!selfScopeWithUnassigned && reqVO.getBusinessLeader() != null, CatalogTableDO::getBusinessLeader, reqVO.getBusinessLeader())
                .apply(deptScopeWithUnassigned, "(t.RESPONSIBLE_DEPT = {0} OR (t.BUSINESS_LEADER IS NULL AND t.RESPONSIBLE_DEPT IS NULL))", reqVO.getResponsibleDept())
                .eq(!deptScopeWithUnassigned && reqVO.getResponsibleDept() != null, CatalogTableDO::getResponsibleDept, reqVO.getResponsibleDept())
                .in(CollectionUtils.isNotEmpty(reqVO.getDbIdList()),CatalogTableDO::getDbId, reqVO.getDbIdList())
                .and(StringUtils.isNotBlank(reqVO.getKeyWord()), wrapper ->
                        wrapper.like(CatalogTableDO::getTableName, reqVO.getKeyWord())
                                .or()
                                .like(CatalogTableDO::getTableComment, reqVO.getKeyWord()))
                .eq(reqVO.getProjectId() != null, CatalogTableDO::getProjectId, reqVO.getProjectId())
                .orderByStr(StringUtils.isNotBlank(reqVO.getOrderByColumn()), StringUtils.equals("asc", reqVO.getIsAsc()), StringUtils.isNotBlank(reqVO.getOrderByColumn()) ? Arrays.asList(reqVO.getOrderByColumn().split(",")) : null);
        // 构造动态查询条件
        return selectPage(reqVO, lambdaWrapperX);
    }
    default List<CatalogTableDO> getCatalogTableListAsset(CatalogTablePageReqVO reqVO){
        boolean selfScopeWithUnassigned = BizDataScopeQueryHelper.useSelfScopeWithUnassigned(
                reqVO.getBizScopeMode(), reqVO.getBizScopeIncludeUnassigned(), reqVO.getBusinessLeader());
        boolean deptScopeWithUnassigned = BizDataScopeQueryHelper.useDeptScopeWithUnassigned(
                reqVO.getBizScopeMode(), reqVO.getBizScopeIncludeUnassigned(), reqVO.getResponsibleDept());
        MPJLambdaWrapperX<CatalogTableDO> lambdaWrapperX = new MPJLambdaWrapperX<>();

        lambdaWrapperX.selectAll(CatalogTableDO.class)
                .select("t2.CRON_EXPRESSION as cronExpression")
                .select("COALESCE(c1.columnCount, 0) as columnCount")
                .leftJoin("Catalog_TASK_SCHEDULER t2 on t.TASK_ID = t2.TASK_ID and t2.DEL_FLAG = '0'")
                .leftJoin("(SELECT TABLE_ID, COUNT(1) AS columnCount FROM Catalog_COLUMN WHERE DEL_FLAG = '0' GROUP BY TABLE_ID) c1 on c1.TABLE_ID = t.ID");

        lambdaWrapperX
                .eq(reqVO.getTaskId() != null, CatalogTableDO::getTaskId, reqVO.getTaskId())
                .apply(selfScopeWithUnassigned, "(t.BUSINESS_LEADER = {0} OR (t.BUSINESS_LEADER IS NULL AND t.RESPONSIBLE_DEPT IS NULL))", reqVO.getBusinessLeader())
                .eq(!selfScopeWithUnassigned && reqVO.getBusinessLeader() != null, CatalogTableDO::getBusinessLeader, reqVO.getBusinessLeader())
                .apply(deptScopeWithUnassigned, "(t.RESPONSIBLE_DEPT = {0} OR (t.BUSINESS_LEADER IS NULL AND t.RESPONSIBLE_DEPT IS NULL))", reqVO.getResponsibleDept())
                .eq(!deptScopeWithUnassigned && reqVO.getResponsibleDept() != null, CatalogTableDO::getResponsibleDept, reqVO.getResponsibleDept())
                .eq(StringUtils.isNotBlank(reqVO.getPortalVisible()), CatalogTableDO::getPortalVisible, reqVO.getPortalVisible())
                .in(CollectionUtils.isNotEmpty(reqVO.getDbIdList()), CatalogTableDO::getDbId, reqVO.getDbIdList())
                .and(StringUtils.isNotBlank(reqVO.getKeyWord()), wrapper ->
                        wrapper.like(CatalogTableDO::getTableName, reqVO.getKeyWord())
                                .or()
                                .like(CatalogTableDO::getTableComment, reqVO.getKeyWord()))
                .eq(reqVO.getProjectId() != null, CatalogTableDO::getProjectId, reqVO.getProjectId())
                .orderByStr(
                        StringUtils.isNotBlank(reqVO.getOrderByColumn()),
                        StringUtils.equals("asc", reqVO.getIsAsc()),
                        StringUtils.isNotBlank(reqVO.getOrderByColumn())
                                ? Arrays.asList(reqVO.getOrderByColumn().split(","))
                                : null
                );

        return selectList(lambdaWrapperX);
    }

    default CatalogTableDO findById(Long id) {
        MPJLambdaWrapperX<CatalogTableDO> lambdaWrapperX = new MPJLambdaWrapperX<>();
        lambdaWrapperX.selectAll(CatalogTableDO.class)
                .select("(SELECT u.NICK_NAME FROM SYSTEM_USER u WHERE u.USER_ID = t.BUSINESS_LEADER) businessLeaderName")
                .select("(SELECT n.NICK_NAME FROM SYSTEM_USER n WHERE n.USER_ID = t.TECH_LEADER) techLeaderName")
//                .select("(SELECT s.SENSITIVE_LEVEL FROM DG_SENSITIVE_LEVEL s WHERE s.ID = t.SAFETY_LEVEL_ID) safetyLevelName")
                .eq(CatalogTableDO::getId, id);
        return selectOne(lambdaWrapperX);
    }


    default boolean existsByDbId(Long dbId) {
        return exists(Wrappers.lambdaQuery(CatalogTableDO.class)
                .eq(CatalogTableDO::getDbId, dbId));
    }

    default boolean existsByDbIds(Collection<Long> dbIds) {
        return exists(Wrappers.lambdaQuery(CatalogTableDO.class)
                .in(CatalogTableDO::getDbId, dbIds));
    }
}
