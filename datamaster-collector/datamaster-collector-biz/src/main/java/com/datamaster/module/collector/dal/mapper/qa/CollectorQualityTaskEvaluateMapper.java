

package com.datamaster.module.collector.dal.mapper.qa;

import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.collector.controller.admin.qa.vo.CollectorQualityTaskEvaluatePageReqVO;
import com.datamaster.module.collector.dal.dataobject.qa.CollectorQualityTaskEvaluateDO;
import com.datamaster.mybatis.core.mapper.BaseMapperX;
import com.datamaster.mybatis.core.query.LambdaQueryWrapperX;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * 数据质量任务-评测规则Mapper接口
 *
 * @author Chaos
 * @date 2025-07-21
 */
public interface CollectorQualityTaskEvaluateMapper extends BaseMapperX<CollectorQualityTaskEvaluateDO> {

    default PageResult<CollectorQualityTaskEvaluateDO> selectPage(CollectorQualityTaskEvaluatePageReqVO reqVO) {
        // 定义排序的字段（防止 SQL 注入，与数据库字段名称一致）
        Set<String> allowedColumns = new HashSet<>(Arrays.asList("id", "create_time", "update_time"));

        // 构造动态查询条件
        return selectPage(reqVO, new LambdaQueryWrapperX<CollectorQualityTaskEvaluateDO>()
                .eqIfPresent(CollectorQualityTaskEvaluateDO::getTaskId, reqVO.getTaskId())
                .likeIfPresent(CollectorQualityTaskEvaluateDO::getName, reqVO.getName())
                .eqIfPresent(CollectorQualityTaskEvaluateDO::getRuleCode, reqVO.getRuleCode())
                .likeIfPresent(CollectorQualityTaskEvaluateDO::getRuleName, reqVO.getRuleName())
                .eqIfPresent(CollectorQualityTaskEvaluateDO::getWarningLevel, reqVO.getWarningLevel())
                .eqIfPresent(CollectorQualityTaskEvaluateDO::getStatus, reqVO.getStatus())
                .eqIfPresent(CollectorQualityTaskEvaluateDO::getRuleDescription, reqVO.getRuleDescription())
                .eqIfPresent(CollectorQualityTaskEvaluateDO::getErrDescription, reqVO.getErrDescription())
                .eqIfPresent(CollectorQualityTaskEvaluateDO::getSuggestion, reqVO.getSuggestion())
                .eqIfPresent(CollectorQualityTaskEvaluateDO::getWhereClause, reqVO.getWhereClause())
                .eqIfPresent(CollectorQualityTaskEvaluateDO::getObjId, reqVO.getObjId())
                .likeIfPresent(CollectorQualityTaskEvaluateDO::getObjName, reqVO.getObjName())
                .likeIfPresent(CollectorQualityTaskEvaluateDO::getTableName, reqVO.getTableName())
                .eqIfPresent(CollectorQualityTaskEvaluateDO::getEvaColumn, reqVO.getEvaColumn())
                .eqIfPresent(CollectorQualityTaskEvaluateDO::getRule, reqVO.getRule())
                .eqIfPresent(CollectorQualityTaskEvaluateDO::getCreateTime, reqVO.getCreateTime())
                // 如果 reqVO.getName() 不为空，则添加 name 的精确匹配条件（name = '<name>'）
                // .likeIfPresent(CollectorQualityTaskEvaluateDO::getName, reqVO.getName())
                // 按照 createTime 字段降序排序
                .orderBy(reqVO.getOrderByColumn(), reqVO.getIsAsc(), allowedColumns));
    }
}
