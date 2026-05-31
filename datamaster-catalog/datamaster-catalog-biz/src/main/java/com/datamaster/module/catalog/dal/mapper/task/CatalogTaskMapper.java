package com.datamaster.module.catalog.dal.mapper.task;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.annotations.Select;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.catalog.controller.admin.task.vo.CatalogTaskPageReqVO;
import com.datamaster.module.catalog.dal.dataobject.task.CatalogTaskDO;
import com.datamaster.mybatis.core.mapper.BaseMapperX;
import com.datamaster.mybatis.core.query.BizDataScopeQueryHelper;

import java.util.Arrays;
import java.util.List;

/**
 * 采集任务Mapper接口
 *
 * @author DATAMASTER
 * @date 2025-12-16
 */
public interface CatalogTaskMapper extends BaseMapperX<CatalogTaskDO> {

    default PageResult<CatalogTaskDO> selectPage(CatalogTaskPageReqVO reqVO) {
        boolean selfScopeWithUnassigned = BizDataScopeQueryHelper.useSelfScopeWithUnassigned(
                reqVO.getBizScopeMode(), reqVO.getBizScopeIncludeUnassigned(), reqVO.getLeader());
        boolean deptScopeWithUnassigned = BizDataScopeQueryHelper.useDeptScopeWithUnassigned(
                reqVO.getBizScopeMode(), reqVO.getBizScopeIncludeUnassigned(), reqVO.getResponsibleDept());
        MPJLambdaWrapper<CatalogTaskDO> lambdaWrapper = new MPJLambdaWrapper();

        lambdaWrapper.selectAll(CatalogTaskDO.class)
                .select("t2.NAME AS sourceSystemName"
                        , "t3.STATUS AS schedulerStatus"
                        ,"t3.CRON_EXPRESSION AS cronExpression"
                        ,"t4.DATASOURCE_NAME AS datasourceName"
                        ,"t4.DATASOURCE_TYPE AS datasourceType"
                        ,"t5.NICK_NAME AS personChargeName"
                )
                .leftJoin("TAX_SOURCE_SYSTEM t2 on t.SOURCE_SYSTEM_ID = t2.ID AND t2.DEL_FLAG = '0'")
                .leftJoin("Catalog_TASK_SCHEDULER t3 ON t.id = t3.task_id AND t3.DEL_FLAG = '0'")
                .leftJoin("AST_DATASOURCE t4 ON t.datasource_id = t4.id AND t4.DEL_FLAG = '0'")
                .leftJoin("SYSTEM_USER t5 ON t.LEADER = t5.USER_ID AND t5.DEL_FLAG = '0'")
                .eq(reqVO.getId() != null,CatalogTaskDO::getId, reqVO.getId())
                .eq(reqVO.getSourceSystemId() != null,CatalogTaskDO::getSourceSystemId, reqVO.getSourceSystemId())
                .likeRight(StringUtils.isNotBlank(reqVO.getSourceSystemName()), CatalogTaskDO::getSourceSystemName, reqVO.getSourceSystemName())
                .like(StringUtils.isNotEmpty( reqVO.getName()), CatalogTaskDO::getName, reqVO.getName())
                .eq( reqVO.getDatasourceId() != null, CatalogTaskDO::getDatasourceId, reqVO.getDatasourceId())
                .eq(StringUtils.isNotEmpty( reqVO.getDbType()), CatalogTaskDO::getDbType, reqVO.getDbType())
                .eq(StringUtils.isNotEmpty( reqVO.getLeaderPhone()), CatalogTaskDO::getLeaderPhone, reqVO.getLeaderPhone())
                .eq(StringUtils.isNotEmpty( reqVO.getCollectionMode()),CatalogTaskDO::getCollectionMode, reqVO.getCollectionMode())
                .eq(StringUtils.isNotEmpty( reqVO.getCollectionScope()),CatalogTaskDO::getCollectionScope, reqVO.getCollectionScope())
                .eq(StringUtils.isNotEmpty( reqVO.getStatus()),CatalogTaskDO::getStatus, reqVO.getStatus())
                .apply(selfScopeWithUnassigned, "(t.LEADER = {0} OR (t.LEADER IS NULL AND t.RESPONSIBLE_DEPT IS NULL))", reqVO.getLeader())
                .eq(!selfScopeWithUnassigned && reqVO.getLeader() != null, CatalogTaskDO::getLeader, reqVO.getLeader())
                .apply(deptScopeWithUnassigned, "(t.RESPONSIBLE_DEPT = {0} OR (t.LEADER IS NULL AND t.RESPONSIBLE_DEPT IS NULL))", reqVO.getResponsibleDept())
                .eq(!deptScopeWithUnassigned && reqVO.getResponsibleDept() != null, CatalogTaskDO::getResponsibleDept, reqVO.getResponsibleDept())
                .ge(reqVO.getCreateTimeStart() != null,
                        CatalogTaskDO::getCreateTime, reqVO.getCreateTimeStart())
                .le(reqVO.getCreateTimeEnd() != null,
                        CatalogTaskDO::getCreateTime, reqVO.getCreateTimeEnd())
                .orderByStr(StringUtils.isNotBlank(reqVO.getOrderByColumn()), StringUtils.equals("asc", reqVO.getIsAsc()), StringUtils.isNotBlank(reqVO.getOrderByColumn()) ? Arrays.asList(reqVO.getOrderByColumn().split(",")) : null);
        return selectJoinPage(reqVO, CatalogTaskDO.class, lambdaWrapper);
    }


    default boolean existsBySourceSystemName(String sourceSystemName) {
        return exists(Wrappers.lambdaQuery(CatalogTaskDO.class)
                .eq(CatalogTaskDO::getDelFlag, "0")
                .likeRight(CatalogTaskDO::getSourceSystemName, sourceSystemName));
    }

    default boolean existsBydataSourceID(Long datasourceId) {
        return exists(Wrappers.lambdaQuery(CatalogTaskDO.class)
                .eq(CatalogTaskDO::getDelFlag, "0")
                .eq(CatalogTaskDO::getDatasourceId, datasourceId));
    }

    /**
     * 检查是否存在指定数据源的任务(排除指定任务ID)
     *
     * @param datasourceId   数据源ID
     * @param excludeTaskId  排除的任务ID(用于更新时排除自身)
     * @return 是否存在
     */
    default boolean existsByDatasourceId(Long datasourceId, Long excludeTaskId) {
        return exists(Wrappers.lambdaQuery(CatalogTaskDO.class)
                .eq(CatalogTaskDO::getDelFlag, "0")
                .eq(CatalogTaskDO::getDatasourceId, datasourceId)
                .ne(excludeTaskId != null, CatalogTaskDO::getId, excludeTaskId));
    }

    /**
     * 检查是否存在指定数据源和采集范围的任务(排除指定任务ID)
     *
     * @param datasourceId   数据源ID
     * @param collectionScope 采集范围
     * @param excludeTaskId  排除的任务ID(用于更新时排除自身)
     * @return 是否存在
     */
    default boolean existsByDatasourceAndScope(Long datasourceId, String collectionScope, Long excludeTaskId) {
        return exists(Wrappers.lambdaQuery(CatalogTaskDO.class)
                .eq(CatalogTaskDO::getDelFlag, "0")
                .eq(CatalogTaskDO::getDatasourceId, datasourceId)
                .eq(CatalogTaskDO::getCollectionScope, collectionScope)
                .ne(excludeTaskId != null, CatalogTaskDO::getId, excludeTaskId));
    }

    /**
     * 查询指定数据源和采集范围的任务列表(排除指定任务ID)
     *
     * @param datasourceId   数据源ID
     * @param collectionScope 采集范围
     * @param excludeTaskId  排除的任务ID(用于更新时排除自身)
     * @return 任务列表
     */
    default List<CatalogTaskDO> selectByDatasourceAndScope(Long datasourceId, String collectionScope, Long excludeTaskId) {
        return selectList(Wrappers.lambdaQuery(CatalogTaskDO.class)
                .eq(CatalogTaskDO::getDelFlag, "0")
                .eq(CatalogTaskDO::getDatasourceId, datasourceId)
                .eq(CatalogTaskDO::getCollectionScope, collectionScope)
                .ne(excludeTaskId != null, CatalogTaskDO::getId, excludeTaskId));
    }

    @InterceptorIgnore(tenantLine = "true")
    @Select("select * from Catalog_TASK where del_flag = '0' and id = #{taskId}")
    CatalogTaskDO getByTaskId(Long taskId);
}
