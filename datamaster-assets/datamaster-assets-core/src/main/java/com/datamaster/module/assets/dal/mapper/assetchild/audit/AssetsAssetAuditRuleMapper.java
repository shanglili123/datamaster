package com.datamaster.module.assets.dal.mapper.assetchild.audit;

import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.assets.controller.admin.assetchild.audit.vo.AssetsAssetAuditRulePageReqVO;
import com.datamaster.module.assets.dal.dataobject.assetchild.audit.AssetsAssetAuditRuleDO;
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
public interface AssetsAssetAuditRuleMapper extends BaseMapperX<AssetsAssetAuditRuleDO> {

    default PageResult<AssetsAssetAuditRuleDO> selectPage(AssetsAssetAuditRulePageReqVO reqVO) {
        // 定义排序的字段（防止 SQL 注入，与数据库字段名称一致）
        Set<String> allowedColumns = new HashSet<>(Arrays.asList("id", "create_time", "update_time"));

        // 构造动态查询条件
        return selectPage(reqVO, new LambdaQueryWrapperX<AssetsAssetAuditRuleDO>()
                .eqIfPresent(AssetsAssetAuditRuleDO::getAssetId, reqVO.getAssetId())
                .likeIfPresent(AssetsAssetAuditRuleDO::getTableName, reqVO.getTableName())
                .likeIfPresent(AssetsAssetAuditRuleDO::getColumnName, reqVO.getColumnName())
                .eqIfPresent(AssetsAssetAuditRuleDO::getColumnComment, reqVO.getColumnComment())
                .likeIfPresent(AssetsAssetAuditRuleDO::getRuleName, reqVO.getRuleName())
                .eqIfPresent(AssetsAssetAuditRuleDO::getQualityDim, reqVO.getQualityDim())
                .eqIfPresent(AssetsAssetAuditRuleDO::getRuleType, reqVO.getRuleType())
                .eqIfPresent(AssetsAssetAuditRuleDO::getRuleLevel, reqVO.getRuleLevel())
                .eqIfPresent(AssetsAssetAuditRuleDO::getRuleDescription, reqVO.getRuleDescription())
                .eqIfPresent(AssetsAssetAuditRuleDO::getRuleConfig, reqVO.getRuleConfig())
                .eqIfPresent(AssetsAssetAuditRuleDO::getTotalCount, reqVO.getTotalCount())
                .eqIfPresent(AssetsAssetAuditRuleDO::getIssueCount, reqVO.getIssueCount())
                .eqIfPresent(AssetsAssetAuditRuleDO::getAuditTime, reqVO.getAuditTime())
                .eqIfPresent(AssetsAssetAuditRuleDO::getBatchNo, reqVO.getBatchNo())
                .eqIfPresent(AssetsAssetAuditRuleDO::getCreateTime, reqVO.getCreateTime())
                // 如果 reqVO.getName() 不为空，则添加 name 的精确匹配条件（name = '<name>'）
                // .likeIfPresent(AssetsAssetAuditRuleDO::getName, reqVO.getName())
                // 按照 createTime 字段降序排序
                .orderBy(reqVO.getOrderByColumn(), reqVO.getIsAsc(), allowedColumns));
    }
}
