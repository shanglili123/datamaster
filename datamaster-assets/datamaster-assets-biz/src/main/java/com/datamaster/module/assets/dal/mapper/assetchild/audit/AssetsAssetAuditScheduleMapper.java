package com.datamaster.module.assets.dal.mapper.assetchild.audit;

import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.assets.controller.admin.assetchild.audit.vo.AssetsAssetAuditSchedulePageReqVO;
import com.datamaster.module.assets.dal.dataobject.assetchild.audit.AssetsAssetAuditScheduleDO;
import com.datamaster.mybatis.core.mapper.BaseMapperX;
import com.datamaster.mybatis.core.query.LambdaQueryWrapperX;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Mapper
 *
 * @author DATAMASTER
 * @date 2025-05-09
 */
public interface AssetsAssetAuditScheduleMapper extends BaseMapperX<AssetsAssetAuditScheduleDO> {

    default PageResult<AssetsAssetAuditScheduleDO> selectPage(AssetsAssetAuditSchedulePageReqVO reqVO) {
        // 定义排序的字段（防止 SQL 注入，与数据库字段名称一致）
        Set<String> allowedColumns = new HashSet<>(Arrays.asList("id", "create_time", "update_time"));

        // 构造动态查询条件
        return selectPage(reqVO, new LambdaQueryWrapperX<AssetsAssetAuditScheduleDO>()
                .eqIfPresent(AssetsAssetAuditScheduleDO::getAssetId, reqVO.getAssetId())
                .eqIfPresent(AssetsAssetAuditScheduleDO::getScheduleFlag, reqVO.getScheduleFlag())
                .eqIfPresent(AssetsAssetAuditScheduleDO::getCronExpression, reqVO.getCronExpression())
                .eqIfPresent(AssetsAssetAuditScheduleDO::getNodeId, reqVO.getNodeId())
                .eqIfPresent(AssetsAssetAuditScheduleDO::getNodeCode, reqVO.getNodeCode())
                .eqIfPresent(AssetsAssetAuditScheduleDO::getTaskId, reqVO.getTaskId())
                .eqIfPresent(AssetsAssetAuditScheduleDO::getTaskCode, reqVO.getTaskCode())
                .eqIfPresent(AssetsAssetAuditScheduleDO::getSystemJobId, reqVO.getSystemJobId())
                .eqIfPresent(AssetsAssetAuditScheduleDO::getCreateTime, reqVO.getCreateTime())
                // 如果 reqVO.getName() 不为空，则添加 name 的精确匹配条件（name = '<name>'）
                // .likeIfPresent(AssetsAssetAuditScheduleDO::getName, reqVO.getName())
                // 按照 createTime 字段降序排序
                .orderBy(reqVO.getOrderByColumn(), reqVO.getIsAsc(), allowedColumns));
    }
}
