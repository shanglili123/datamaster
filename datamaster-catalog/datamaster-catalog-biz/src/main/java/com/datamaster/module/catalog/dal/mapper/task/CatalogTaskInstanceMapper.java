package com.datamaster.module.catalog.dal.mapper.task;

import com.github.yulichang.wrapper.MPJLambdaWrapper;
import org.apache.commons.lang3.StringUtils;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.catalog.controller.admin.task.vo.CatalogTaskInstancePageReqVO;
import com.datamaster.module.catalog.dal.dataobject.task.CatalogTaskDO;
import com.datamaster.module.catalog.dal.dataobject.task.CatalogTaskInstanceDO;
import com.datamaster.mybatis.core.mapper.BaseMapperX;

import java.util.Arrays;

/**
 * 采集任务实例Mapper接口
 *
 * @author DATAMASTER
 * @date 2025-12-16
 */
public interface CatalogTaskInstanceMapper extends BaseMapperX<CatalogTaskInstanceDO> {

//    default PageResult<CatalogTaskInstanceDO> selectPage(CatalogTaskInstancePageReqVO reqVO) {
//        // 定义排序的字段（防止 SQL 注入，与数据库字段名称一致）
//        Set<String> allowedColumns = new HashSet<>(Arrays.asList("id", "create_time", "update_time"));
//
//        // 构造动态查询条件
//        return selectPage(reqVO, new LambdaQueryWrapperX<CatalogTaskInstanceDO>()
//                .eqIfPresent(CatalogTaskInstanceDO::getDomainId, reqVO.getDomainId())
//                .eqIfPresent(CatalogTaskInstanceDO::getDomainCode, reqVO.getDomainCode())
//                .eqIfPresent(CatalogTaskInstanceDO::getTaskId, reqVO.getTaskId())
//                .eqIfPresent(CatalogTaskInstanceDO::getCollectionMode, reqVO.getCollectionMode())
//                .eqIfPresent(CatalogTaskInstanceDO::getCollectionScope, reqVO.getCollectionScope())
//                .eqIfPresent(CatalogTaskInstanceDO::getTotalCount, reqVO.getTotalCount())
//                .eqIfPresent(CatalogTaskInstanceDO::getSuccessCount, reqVO.getSuccessCount())
//                .eqIfPresent(CatalogTaskInstanceDO::getFailCount, reqVO.getFailCount())
//                .eqIfPresent(CatalogTaskInstanceDO::getFailCause, reqVO.getFailCause())
//                .eqIfPresent(CatalogTaskInstanceDO::getAddCount, reqVO.getAddCount())
//                .eqIfPresent(CatalogTaskInstanceDO::getDelCount, reqVO.getDelCount())
//                .eqIfPresent(CatalogTaskInstanceDO::getUpdateCount, reqVO.getUpdateCount())
//                .eqIfPresent(CatalogTaskInstanceDO::getStartTime, reqVO.getStartTime())
//                .eqIfPresent(CatalogTaskInstanceDO::getEndTime, reqVO.getEndTime())
//                .eqIfPresent(CatalogTaskInstanceDO::getDuration, reqVO.getDuration())
//                .eqIfPresent(CatalogTaskInstanceDO::getStatus, reqVO.getStatus())
//                .eqIfPresent(CatalogTaskInstanceDO::getCreateTime, reqVO.getCreateTime())
//                .eqIfPresent(CatalogTaskInstanceDO::getDescription, reqVO.getDescription())
//                // 如果 reqVO.getName() 不为空，则添加 name 的精确匹配条件（name = '<name>'）
//                // .likeIfPresent(CatalogTaskInstanceDO::getName, reqVO.getName())
//                // 按照 createTime 字段降序排序
//                .orderBy(reqVO.getOrderByColumn(), reqVO.getIsAsc(), allowedColumns));
//    }

    default PageResult<CatalogTaskInstanceDO> selectPage(CatalogTaskInstancePageReqVO reqVO) {

        MPJLambdaWrapper<CatalogTaskInstanceDO> lambdaWrapper = new MPJLambdaWrapper();

        lambdaWrapper.selectAll(CatalogTaskInstanceDO.class)
                .select("t2.NAME AS sourceSystemName"
                        , "t3.NAME AS name"
                        , "t3.STATUS AS taskStatus"
                        ,"t4.DATASOURCE_NAME AS datasourceName"
                        ,"t4.DATASOURCE_TYPE AS datasourceType"
                )
                .leftJoin("TAX_SOURCE_SYSTEM t2 on t.SOURCE_SYSTEM_ID = t2.ID AND t2.DEL_FLAG = '0'")
                .leftJoin("Catalog_TASK t3 ON t3.id = t.task_id")
                .leftJoin("AST_DATASOURCE t4 ON t3.datasource_id = t4.id AND t4.DEL_FLAG = '0'")
                .eq(reqVO.getDatasourceId() != null,"t3.DATASOURCE_ID", reqVO.getDatasourceId())
                .eq(reqVO.getSourceSystemId() != null,CatalogTaskInstanceDO::getSourceSystemId, reqVO.getSourceSystemId())
                .eq(reqVO.getTaskId() != null,CatalogTaskInstanceDO::getTaskId, reqVO.getTaskId())
                .likeRight(StringUtils.isNotBlank(reqVO.getSourceSystemName()), CatalogTaskDO::getSourceSystemName, reqVO.getSourceSystemName())
                .like(StringUtils.isNotEmpty( reqVO.getName()), "t3.NAME", reqVO.getName())
                .eq(StringUtils.isNotEmpty( reqVO.getCollectionMode()),CatalogTaskInstanceDO::getCollectionMode, reqVO.getCollectionMode())
                .eq(StringUtils.isNotEmpty( reqVO.getCollectionScope()),CatalogTaskInstanceDO::getCollectionScope, reqVO.getCollectionScope())
                .eq(StringUtils.isNotEmpty( reqVO.getStatus()),CatalogTaskInstanceDO::getStatus, reqVO.getStatus())
                .eq(StringUtils.isNotEmpty( reqVO.getValidFlag()),CatalogTaskInstanceDO::getValidFlag, reqVO.getValidFlag())
                .ge(reqVO.getCreateTimeStart() != null,
                        CatalogTaskInstanceDO::getCreateTime, reqVO.getCreateTimeStart())
                .le(reqVO.getCreateTimeEnd() != null,
                        CatalogTaskInstanceDO::getCreateTime, reqVO.getCreateTimeEnd())
                .orderByStr(StringUtils.isNotBlank(reqVO.getOrderByColumn()), StringUtils.equals("asc", reqVO.getIsAsc()), StringUtils.isNotBlank(reqVO.getOrderByColumn()) ? Arrays.asList(reqVO.getOrderByColumn().split(",")) : null);
        return selectJoinPage(reqVO, CatalogTaskInstanceDO.class, lambdaWrapper);


    }
}
