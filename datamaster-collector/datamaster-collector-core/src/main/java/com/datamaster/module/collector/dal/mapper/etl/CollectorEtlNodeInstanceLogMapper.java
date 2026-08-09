

package com.datamaster.module.collector.dal.mapper.etl;

import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.collector.controller.admin.etl.vo.CollectorEtlNodeInstanceLogPageReqVO;
import com.datamaster.module.collector.dal.dataobject.etl.CollectorEtlNodeInstanceLogDO;
import com.datamaster.mybatis.core.mapper.BaseMapperX;
import com.datamaster.mybatis.core.query.LambdaQueryWrapperX;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * 数据集成节点实例-日志Mapper接口
 *
 * @author DATAMASTER
 * @date 2025-08-05
 */
public interface CollectorEtlNodeInstanceLogMapper extends BaseMapperX<CollectorEtlNodeInstanceLogDO> {

    default PageResult<CollectorEtlNodeInstanceLogDO> selectPage(CollectorEtlNodeInstanceLogPageReqVO reqVO) {
        // 定义排序的字段（防止 SQL 注入，与数据库字段名称一致）
        Set<String> allowedColumns = new HashSet<>(Arrays.asList("id", "create_time", "update_time"));

        // 构造动态查询条件
        return selectPage(reqVO, new LambdaQueryWrapperX<CollectorEtlNodeInstanceLogDO>()
                .eqIfPresent(CollectorEtlNodeInstanceLogDO::getTaskType, reqVO.getTaskType())
                .eqIfPresent(CollectorEtlNodeInstanceLogDO::getNodeId, reqVO.getNodeId())
                .eqIfPresent(CollectorEtlNodeInstanceLogDO::getNodeCode, reqVO.getNodeCode())
                .eqIfPresent(CollectorEtlNodeInstanceLogDO::getTaskInstanceId, reqVO.getTaskInstanceId())
                .eqIfPresent(CollectorEtlNodeInstanceLogDO::getLogContent, reqVO.getLogContent())
                .eqIfPresent(CollectorEtlNodeInstanceLogDO::getCreateTime, reqVO.getCreateTime())
                // 如果 reqVO.getName() 不为空，则添加 name 的精确匹配条件（name = '<name>'）
                // .likeIfPresent(CollectorEtlNodeInstanceLogDO::getName, reqVO.getName())
                // 按照 createTime 字段降序排序
                .orderBy(reqVO.getOrderByColumn(), reqVO.getIsAsc(), allowedColumns));
    }
}
