package com.datamaster.module.catalog.dal.mapper.task;

import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.catalog.controller.admin.task.vo.CatalogTaskSchedulerPageReqVO;
import com.datamaster.module.catalog.dal.dataobject.task.CatalogTaskSchedulerDO;
import com.datamaster.mybatis.core.mapper.BaseMapperX;
import com.datamaster.mybatis.core.query.LambdaQueryWrapperX;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * 数据集成调度信息Mapper接口
 *
 * @author DATAMASTER
 * @date 2025-12-16
 */
public interface CatalogTaskSchedulerMapper extends BaseMapperX<CatalogTaskSchedulerDO> {

    default PageResult<CatalogTaskSchedulerDO> selectPage(CatalogTaskSchedulerPageReqVO reqVO) {
        // 定义排序的字段（防止 SQL 注入，与数据库字段名称一致）
        Set<String> allowedColumns = new HashSet<>(Arrays.asList("id", "create_time", "update_time"));

        // 构造动态查询条件
        return selectPage(reqVO, new LambdaQueryWrapperX<CatalogTaskSchedulerDO>()
                .eqIfPresent(CatalogTaskSchedulerDO::getTaskId, reqVO.getTaskId())
                .eqIfPresent(CatalogTaskSchedulerDO::getJobId, reqVO.getJobId())
                .eqIfPresent(CatalogTaskSchedulerDO::getStartTime, reqVO.getStartTime())
                .eqIfPresent(CatalogTaskSchedulerDO::getEndTime, reqVO.getEndTime())
                .eqIfPresent(CatalogTaskSchedulerDO::getTimezoneId, reqVO.getTimezoneId())
                .eqIfPresent(CatalogTaskSchedulerDO::getCronExpression, reqVO.getCronExpression())
                .eqIfPresent(CatalogTaskSchedulerDO::getFailureStrategy, reqVO.getFailureStrategy())
                .eqIfPresent(CatalogTaskSchedulerDO::getStatus, reqVO.getStatus())
                .eqIfPresent(CatalogTaskSchedulerDO::getCreateTime, reqVO.getCreateTime())
                // 如果 reqVO.getName() 不为空，则添加 name 的精确匹配条件（name = '<name>'）
                // .likeIfPresent(CatalogTaskSchedulerDO::getName, reqVO.getName())
                // 按照 createTime 字段降序排序
                .orderBy(reqVO.getOrderByColumn(), reqVO.getIsAsc(), allowedColumns));
    }
}
