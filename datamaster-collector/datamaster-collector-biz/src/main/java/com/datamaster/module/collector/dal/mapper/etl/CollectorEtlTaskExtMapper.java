

package com.datamaster.module.collector.dal.mapper.etl;

import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.collector.controller.admin.etl.vo.CollectorEtlTaskExtPageReqVO;
import com.datamaster.module.collector.dal.dataobject.etl.CollectorEtlTaskExtDO;
import com.datamaster.mybatis.core.mapper.BaseMapperX;
import com.datamaster.mybatis.core.query.LambdaQueryWrapperX;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * 数据集成任务-扩展数据Mapper接口
 *
 * @author DATAMASTER
 * @date 2025-04-16
 */
public interface CollectorEtlTaskExtMapper extends BaseMapperX<CollectorEtlTaskExtDO> {

    default PageResult<CollectorEtlTaskExtDO> selectPage(CollectorEtlTaskExtPageReqVO reqVO) {
        // 定义排序的字段（防止 SQL 注入，与数据库字段名称一致）
        Set<String> allowedColumns = new HashSet<>(Arrays.asList("id", "create_time", "update_time"));

        // 构造动态查询条件
        return selectPage(reqVO, new LambdaQueryWrapperX<CollectorEtlTaskExtDO>()
                .eqIfPresent(CollectorEtlTaskExtDO::getTaskId, reqVO.getTaskId())
                .eqIfPresent(CollectorEtlTaskExtDO::getEtlNodeId, reqVO.getEtlNodeId())
                .likeIfPresent(CollectorEtlTaskExtDO::getEtlNodeName, reqVO.getEtlNodeName())
                .eqIfPresent(CollectorEtlTaskExtDO::getEtlNodeCode, reqVO.getEtlNodeCode())
                .eqIfPresent(CollectorEtlTaskExtDO::getEtlNodeVersion, reqVO.getEtlNodeVersion())
                .eqIfPresent(CollectorEtlTaskExtDO::getEtlRelationId, reqVO.getEtlRelationId())
                // 如果 reqVO.getName() 不为空，则添加 name 的精确匹配条件（name = '<name>'）
                // .likeIfPresent(CollectorEtlTaskExtDO::getName, reqVO.getName())
                // 按照 createTime 字段降序排序
                .orderBy(reqVO.getOrderByColumn(), reqVO.getIsAsc(), allowedColumns));
    }
}
