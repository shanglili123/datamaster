

package com.datamaster.module.collector.dal.mapper.etl;

import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.collector.controller.admin.etl.vo.CollectorEtlTaskNodeRelLogPageReqVO;
import com.datamaster.module.collector.dal.dataobject.etl.CollectorEtlTaskNodeRelLogDO;
import com.datamaster.mybatis.core.mapper.BaseMapperX;
import com.datamaster.mybatis.core.query.LambdaQueryWrapperX;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * 数据集成任务节点关系-日志Mapper接口
 *
 * @author DATAMASTER
 * @date 2025-02-13
 */
public interface CollectorEtlTaskNodeRelLogMapper extends BaseMapperX<CollectorEtlTaskNodeRelLogDO> {

    default PageResult<CollectorEtlTaskNodeRelLogDO> selectPage(CollectorEtlTaskNodeRelLogPageReqVO reqVO) {
        // 定义排序的字段（防止 SQL 注入，与数据库字段名称一致）
        Set<String> allowedColumns = new HashSet<>(Arrays.asList("id", "create_time", "update_time"));

        // 构造动态查询条件
        return selectPage(reqVO, new LambdaQueryWrapperX<CollectorEtlTaskNodeRelLogDO>()
                .eqIfPresent(CollectorEtlTaskNodeRelLogDO::getProjectId, reqVO.getProjectId())
                .eqIfPresent(CollectorEtlTaskNodeRelLogDO::getProjectCode, reqVO.getProjectCode())
                .eqIfPresent(CollectorEtlTaskNodeRelLogDO::getTaskId, reqVO.getTaskId())
                .eqIfPresent(CollectorEtlTaskNodeRelLogDO::getTaskCode, reqVO.getTaskCode())
                .eqIfPresent(CollectorEtlTaskNodeRelLogDO::getTaskVersion, reqVO.getTaskVersion())
                .eqIfPresent(CollectorEtlTaskNodeRelLogDO::getPreNodeId, reqVO.getPreNodeId())
                .eqIfPresent(CollectorEtlTaskNodeRelLogDO::getPreNodeCode, reqVO.getPreNodeCode())
                .eqIfPresent(CollectorEtlTaskNodeRelLogDO::getPreNodeVersion, reqVO.getPreNodeVersion())
                .eqIfPresent(CollectorEtlTaskNodeRelLogDO::getPostNodeId, reqVO.getPostNodeId())
                .eqIfPresent(CollectorEtlTaskNodeRelLogDO::getPostNodeCode, reqVO.getPostNodeCode())
                .eqIfPresent(CollectorEtlTaskNodeRelLogDO::getPostNodeVersion, reqVO.getPostNodeVersion())
                .eqIfPresent(CollectorEtlTaskNodeRelLogDO::getCreateTime, reqVO.getCreateTime())
                // 如果 reqVO.getName() 不为空，则添加 name 的精确匹配条件（name = '<name>'）
                // .likeIfPresent(CollectorEtlTaskNodeRelLogDO::getName, reqVO.getName())
                // 按照 createTime 字段降序排序
                .orderBy(reqVO.getOrderByColumn(), reqVO.getIsAsc(), allowedColumns));
    }
}
