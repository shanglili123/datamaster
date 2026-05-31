

package com.datamaster.module.collector.dal.mapper.etl;

import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.collector.controller.admin.etl.vo.CollectorEtlTaskNodeRelPageReqVO;
import com.datamaster.module.collector.dal.dataobject.etl.CollectorEtlTaskNodeRelDO;
import com.datamaster.mybatis.core.mapper.BaseMapperX;
import com.datamaster.mybatis.core.query.LambdaQueryWrapperX;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * 数据集成任务节点关系Mapper接口
 *
 * @author DATAMASTER
 * @date 2025-02-13
 */
public interface CollectorEtlTaskNodeRelMapper extends BaseMapperX<CollectorEtlTaskNodeRelDO> {

    default PageResult<CollectorEtlTaskNodeRelDO> selectPage(CollectorEtlTaskNodeRelPageReqVO reqVO) {
        // 定义排序的字段（防止 SQL 注入，与数据库字段名称一致）
        Set<String> allowedColumns = new HashSet<>(Arrays.asList("id", "create_time", "update_time"));

        // 构造动态查询条件
        return selectPage(reqVO, new LambdaQueryWrapperX<CollectorEtlTaskNodeRelDO>()
                .eqIfPresent(CollectorEtlTaskNodeRelDO::getProjectId, reqVO.getProjectId())
                .eqIfPresent(CollectorEtlTaskNodeRelDO::getProjectCode, reqVO.getProjectCode())
                .eqIfPresent(CollectorEtlTaskNodeRelDO::getTaskId, reqVO.getTaskId())
                .eqIfPresent(CollectorEtlTaskNodeRelDO::getTaskCode, reqVO.getTaskCode())
                .eqIfPresent(CollectorEtlTaskNodeRelDO::getTaskVersion, reqVO.getTaskVersion())
                .eqIfPresent(CollectorEtlTaskNodeRelDO::getPreNodeId, reqVO.getPreNodeId())
                .eqIfPresent(CollectorEtlTaskNodeRelDO::getPreNodeCode, reqVO.getPreNodeCode())
                .eqIfPresent(CollectorEtlTaskNodeRelDO::getPreNodeVersion, reqVO.getPreNodeVersion())
                .eqIfPresent(CollectorEtlTaskNodeRelDO::getPostNodeId, reqVO.getPostNodeId())
                .eqIfPresent(CollectorEtlTaskNodeRelDO::getPostNodeCode, reqVO.getPostNodeCode())
                .eqIfPresent(CollectorEtlTaskNodeRelDO::getPostNodeVersion, reqVO.getPostNodeVersion())
                .eqIfPresent(CollectorEtlTaskNodeRelDO::getCreateTime, reqVO.getCreateTime())
                // 如果 reqVO.getName() 不为空，则添加 name 的精确匹配条件（name = '<name>'）
                // .likeIfPresent(CollectorEtlTaskNodeRelDO::getName, reqVO.getName())
                // 按照 createTime 字段降序排序
                .orderBy(reqVO.getOrderByColumn(), reqVO.getIsAsc(), allowedColumns));
    }
}
