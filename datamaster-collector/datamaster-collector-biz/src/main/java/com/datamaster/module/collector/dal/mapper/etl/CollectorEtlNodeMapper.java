

package com.datamaster.module.collector.dal.mapper.etl;

import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.collector.controller.admin.etl.vo.CollectorEtlNodePageReqVO;
import com.datamaster.module.collector.dal.dataobject.etl.CollectorEtlNodeDO;
import com.datamaster.mybatis.core.mapper.BaseMapperX;
import com.datamaster.mybatis.core.query.LambdaQueryWrapperX;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 数据集成节点Mapper接口
 *
 * @author DATAMASTER
 * @date 2025-02-13
 */
public interface CollectorEtlNodeMapper extends BaseMapperX<CollectorEtlNodeDO> {

    default PageResult<CollectorEtlNodeDO> selectPage(CollectorEtlNodePageReqVO reqVO) {
        // 定义排序的字段（防止 SQL 注入，与数据库字段名称一致）
        Set<String> allowedColumns = new HashSet<>(Arrays.asList("id", "create_time", "update_time"));

        // 构造动态查询条件
        return selectPage(reqVO, new LambdaQueryWrapperX<CollectorEtlNodeDO>()
                .eqIfPresent(CollectorEtlNodeDO::getType, reqVO.getType())
                .likeIfPresent(CollectorEtlNodeDO::getName, reqVO.getName())
                .eqIfPresent(CollectorEtlNodeDO::getCode, reqVO.getCode())
                .eqIfPresent(CollectorEtlNodeDO::getVersion, reqVO.getVersion())
                .eqIfPresent(CollectorEtlNodeDO::getProjectId, reqVO.getProjectId())
                .eqIfPresent(CollectorEtlNodeDO::getProjectCode, reqVO.getProjectCode())
                .eqIfPresent(CollectorEtlNodeDO::getParameters, reqVO.getParameters())
                .eqIfPresent(CollectorEtlNodeDO::getPriority, reqVO.getPriority())
                .eqIfPresent(CollectorEtlNodeDO::getFailRetryTimes, reqVO.getFailRetryTimes())
                .eqIfPresent(CollectorEtlNodeDO::getFailRetryInterval, reqVO.getFailRetryInterval())
                .eqIfPresent(CollectorEtlNodeDO::getTimeout, reqVO.getTimeout())
                .eqIfPresent(CollectorEtlNodeDO::getDelayTime, reqVO.getDelayTime())
                .eqIfPresent(CollectorEtlNodeDO::getCpuQuota, reqVO.getCpuQuota())
                .eqIfPresent(CollectorEtlNodeDO::getMemoryMax, reqVO.getMemoryMax())
                .eqIfPresent(CollectorEtlNodeDO::getDescription, reqVO.getDescription())
                .eqIfPresent(CollectorEtlNodeDO::getDsId, reqVO.getDsId())
                .eqIfPresent(CollectorEtlNodeDO::getCreateTime, reqVO.getCreateTime())
                // 如果 reqVO.getName() 不为空，则添加 name 的精确匹配条件（name = '<name>'）
                // .likeIfPresent(CollectorEtlNodeDO::getName, reqVO.getName())
                // 按照 createTime 字段降序排序
                .orderBy(reqVO.getOrderByColumn(), reqVO.getIsAsc(), allowedColumns));
    }

    void removeOldCollectorEtlNode(List<String> code);
}
