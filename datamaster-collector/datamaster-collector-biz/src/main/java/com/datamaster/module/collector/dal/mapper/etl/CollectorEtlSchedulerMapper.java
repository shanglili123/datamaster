

package com.datamaster.module.collector.dal.mapper.etl;

import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.collector.controller.admin.etl.vo.CollectorEtlSchedulerPageReqVO;
import com.datamaster.module.collector.dal.dataobject.etl.CollectorEtlSchedulerDO;
import com.datamaster.mybatis.core.mapper.BaseMapperX;
import com.datamaster.mybatis.core.query.LambdaQueryWrapperX;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * 数据集成调度信息Mapper接口
 *
 * @author DATAMASTER
 * @date 2025-02-13
 */
public interface CollectorEtlSchedulerMapper extends BaseMapperX<CollectorEtlSchedulerDO> {

    default PageResult<CollectorEtlSchedulerDO> selectPage(CollectorEtlSchedulerPageReqVO reqVO) {
        // 定义排序的字段（防止 SQL 注入，与数据库字段名称一致）
        Set<String> allowedColumns = new HashSet<>(Arrays.asList("id", "create_time", "update_time"));

        // 构造动态查询条件
        return selectPage(reqVO, new LambdaQueryWrapperX<CollectorEtlSchedulerDO>()
                .eqIfPresent(CollectorEtlSchedulerDO::getTaskId, reqVO.getTaskId())
                .eqIfPresent(CollectorEtlSchedulerDO::getTaskCode, reqVO.getTaskCode())
                .eqIfPresent(CollectorEtlSchedulerDO::getStartTime, reqVO.getStartTime())
                .eqIfPresent(CollectorEtlSchedulerDO::getEndTime, reqVO.getEndTime())
                .eqIfPresent(CollectorEtlSchedulerDO::getTimezoneId, reqVO.getTimezoneId())
                .eqIfPresent(CollectorEtlSchedulerDO::getCronExpression, reqVO.getCronExpression())
                .eqIfPresent(CollectorEtlSchedulerDO::getFailureStrategy, reqVO.getFailureStrategy())
                .eqIfPresent(CollectorEtlSchedulerDO::getDsId, reqVO.getDsId())
                .eqIfPresent(CollectorEtlSchedulerDO::getCreateTime, reqVO.getCreateTime())
                // 如果 reqVO.getName() 不为空，则添加 name 的精确匹配条件（name = '<name>'）
                // .likeIfPresent(CollectorEtlSchedulerDO::getName, reqVO.getName())
                // 按照 createTime 字段降序排序
                .orderBy(reqVO.getOrderByColumn(), reqVO.getIsAsc(), allowedColumns));
    }
}
