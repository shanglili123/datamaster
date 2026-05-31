package com.datamaster.module.assets.dal.mapper.discovery;

import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.assets.controller.admin.discovery.vo.AssetsDiscoveryTaskLogPageReqVO;
import com.datamaster.module.assets.dal.dataobject.discovery.AssetsDiscoveryTaskLogDO;
import com.datamaster.mybatis.core.mapper.BaseMapperX;
import com.datamaster.mybatis.core.query.LambdaQueryWrapperX;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Mapper
 *
 * @author DATAMASTER
 * @date 2025-02-17
 */
public interface AssetsDiscoveryTaskLogMapper extends BaseMapperX<AssetsDiscoveryTaskLogDO> {

    default PageResult<AssetsDiscoveryTaskLogDO> selectPage(AssetsDiscoveryTaskLogPageReqVO reqVO) {
        // 定义排序的字段（防止 SQL 注入，与数据库字段名称一致）
        Set<String> allowedColumns = new HashSet<>(Arrays.asList("id", "create_time", "update_time"));

        // 构造动态查询条件
        return selectPage(reqVO, new LambdaQueryWrapperX<AssetsDiscoveryTaskLogDO>()
                .likeIfPresent(AssetsDiscoveryTaskLogDO::getName, reqVO.getName())
                .eqIfPresent(AssetsDiscoveryTaskLogDO::getNodeId, reqVO.getNodeId())
                .eqIfPresent(AssetsDiscoveryTaskLogDO::getNodeCode, reqVO.getNodeCode())
                .likeIfPresent(AssetsDiscoveryTaskLogDO::getTaskName, reqVO.getTaskName())
                .eqIfPresent(AssetsDiscoveryTaskLogDO::getTaskId, reqVO.getTaskId())
                .eqIfPresent(AssetsDiscoveryTaskLogDO::getTaskCode, reqVO.getTaskCode())
                .eqIfPresent(AssetsDiscoveryTaskLogDO::getStartTime, reqVO.getStartTime())
                .eqIfPresent(AssetsDiscoveryTaskLogDO::getEndTime, reqVO.getEndTime())
                .eqIfPresent(AssetsDiscoveryTaskLogDO::getStatus, reqVO.getStatus())
                .eqIfPresent(AssetsDiscoveryTaskLogDO::getNewTableCount, reqVO.getNewTableCount())
                .eqIfPresent(AssetsDiscoveryTaskLogDO::getModifiedTableCount, reqVO.getModifiedTableCount())
                .eqIfPresent(AssetsDiscoveryTaskLogDO::getDeletedTableCount, reqVO.getDeletedTableCount())
                .eqIfPresent(AssetsDiscoveryTaskLogDO::getContact, reqVO.getContact())
                .eqIfPresent(AssetsDiscoveryTaskLogDO::getContactId, reqVO.getContactId())
                .eqIfPresent(AssetsDiscoveryTaskLogDO::getContactNumber, reqVO.getContactNumber())
                .eqIfPresent(AssetsDiscoveryTaskLogDO::getEmail, reqVO.getEmail())
                .eqIfPresent(AssetsDiscoveryTaskLogDO::getDsId, reqVO.getDsId())
                .eqIfPresent(AssetsDiscoveryTaskLogDO::getDsTaskInstanceId, reqVO.getDsTaskInstanceId())
                .eqIfPresent(AssetsDiscoveryTaskLogDO::getPath, reqVO.getPath())
                .eqIfPresent(AssetsDiscoveryTaskLogDO::getCreateTime, reqVO.getCreateTime())
                // 如果 reqVO.getName() 不为空，则添加 name 的精确匹配条件（name = '<name>'）
                // .likeIfPresent(AssetsDiscoveryTaskLogDO::getName, reqVO.getName())
                // 按照 createTime 字段降序排序
                .orderBy(reqVO.getOrderByColumn(), reqVO.getIsAsc(), allowedColumns));
    }
}
