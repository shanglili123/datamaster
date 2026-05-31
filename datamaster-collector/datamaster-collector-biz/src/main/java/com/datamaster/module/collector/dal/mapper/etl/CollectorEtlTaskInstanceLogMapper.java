

package com.datamaster.module.collector.dal.mapper.etl;

import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.collector.controller.admin.etl.vo.CollectorEtlTaskInstanceLogPageReqVO;
import com.datamaster.module.collector.dal.dataobject.etl.CollectorEtlTaskInstanceLogDO;
import com.datamaster.mybatis.core.mapper.BaseMapperX;
import com.datamaster.mybatis.core.query.LambdaQueryWrapperX;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * 数据集成任务实例-日志Mapper接口
 *
 * @author DATAMASTER
 * @date 2025-08-05
 */
public interface CollectorEtlTaskInstanceLogMapper extends BaseMapperX<CollectorEtlTaskInstanceLogDO> {

    default PageResult<CollectorEtlTaskInstanceLogDO> selectPage(CollectorEtlTaskInstanceLogPageReqVO reqVO) {
        // 定义排序的字段（防止 SQL 注入，与数据库字段名称一致）
        Set<String> allowedColumns = new HashSet<>(Arrays.asList("id", "create_time", "update_time"));

        // 构造动态查询条件
        return selectPage(reqVO, new LambdaQueryWrapperX<CollectorEtlTaskInstanceLogDO>()
                .eqIfPresent(CollectorEtlTaskInstanceLogDO::getTaskType, reqVO.getTaskType())
                .eqIfPresent(CollectorEtlTaskInstanceLogDO::getTaskId, reqVO.getTaskId())
                .eqIfPresent(CollectorEtlTaskInstanceLogDO::getTaskCode, reqVO.getTaskCode())
                .eqIfPresent(CollectorEtlTaskInstanceLogDO::getLogContent, reqVO.getLogContent())
                .eqIfPresent(CollectorEtlTaskInstanceLogDO::getCreateTime, reqVO.getCreateTime())
                // 如果 reqVO.getName() 不为空，则添加 name 的精确匹配条件（name = '<name>'）
                // .likeIfPresent(CollectorEtlTaskInstanceLogDO::getName, reqVO.getName())
                // 按照 createTime 字段降序排序
                .orderBy(reqVO.getOrderByColumn(), reqVO.getIsAsc(), allowedColumns));
    }
}
