

package com.datamaster.module.collector.dal.mapper.etl;

import org.apache.ibatis.annotations.Param;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.collector.controller.admin.etl.vo.CollectorEtlNodeLogPageReqVO;
import com.datamaster.module.collector.dal.dataobject.etl.CollectorEtlNodeLogDO;
import com.datamaster.mybatis.core.mapper.BaseMapperX;
import com.datamaster.mybatis.core.query.LambdaQueryWrapperX;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * 数据集成节点-日志Mapper接口
 *
 * @author DATAMASTER
 * @date 2025-02-13
 */
public interface CollectorEtlNodeLogMapper extends BaseMapperX<CollectorEtlNodeLogDO> {

    default PageResult<CollectorEtlNodeLogDO> selectPage(CollectorEtlNodeLogPageReqVO reqVO) {
        // 定义排序的字段（防止 SQL 注入，与数据库字段名称一致）
        Set<String> allowedColumns = new HashSet<>(Arrays.asList("id", "create_time", "update_time"));

        // 构造动态查询条件
        return selectPage(reqVO, new LambdaQueryWrapperX<CollectorEtlNodeLogDO>()
                .eqIfPresent(CollectorEtlNodeLogDO::getType, reqVO.getType())
                .likeIfPresent(CollectorEtlNodeLogDO::getName, reqVO.getName())
                .eqIfPresent(CollectorEtlNodeLogDO::getCode, reqVO.getCode())
                .eqIfPresent(CollectorEtlNodeLogDO::getVersion, reqVO.getVersion())
                .eqIfPresent(CollectorEtlNodeLogDO::getProjectId, reqVO.getProjectId())
                .eqIfPresent(CollectorEtlNodeLogDO::getProjectCode, reqVO.getProjectCode())
                .eqIfPresent(CollectorEtlNodeLogDO::getParameters, reqVO.getParameters())
                .eqIfPresent(CollectorEtlNodeLogDO::getPriority, reqVO.getPriority())
                .eqIfPresent(CollectorEtlNodeLogDO::getFailRetryTimes, reqVO.getFailRetryTimes())
                .eqIfPresent(CollectorEtlNodeLogDO::getFailRetryInterval, reqVO.getFailRetryInterval())
                .eqIfPresent(CollectorEtlNodeLogDO::getTimeout, reqVO.getTimeout())
                .eqIfPresent(CollectorEtlNodeLogDO::getDelayTime, reqVO.getDelayTime())
                .eqIfPresent(CollectorEtlNodeLogDO::getCpuQuota, reqVO.getCpuQuota())
                .eqIfPresent(CollectorEtlNodeLogDO::getMemoryMax, reqVO.getMemoryMax())
                .eqIfPresent(CollectorEtlNodeLogDO::getDescription, reqVO.getDescription())
                .eqIfPresent(CollectorEtlNodeLogDO::getDsId, reqVO.getDsId())
                .eqIfPresent(CollectorEtlNodeLogDO::getCreateTime, reqVO.getCreateTime())
                // 如果 reqVO.getName() 不为空，则添加 name 的精确匹配条件（name = '<name>'）
                // .likeIfPresent(CollectorEtlNodeLogDO::getName, reqVO.getName())
                // 按照 createTime 字段降序排序
                .orderBy(reqVO.getOrderByColumn(), reqVO.getIsAsc(), allowedColumns));
    }

    Integer getMaxVersionByNodeCode(@Param("nodeCode") String nodeCode);
}
