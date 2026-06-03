

package com.datamaster.quality.dal.mapper.qa;


import com.datamaster.common.core.page.PageResult;
import com.datamaster.mybatis.core.mapper.BaseMapperX;
import com.datamaster.mybatis.core.query.LambdaQueryWrapperX;
import com.datamaster.quality.controller.qa.vo.QualityTaskEvaluatePageReqVO;
import com.datamaster.quality.dal.dataobject.qa.QualityTaskEvaluateDO;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * 数据质量任务-评测规则Mapper接口
 *
 * @author Chaos
 * @date 2025-07-21
 */
public interface QualityTaskEvaluateMapper extends BaseMapperX<QualityTaskEvaluateDO> {

    default PageResult<QualityTaskEvaluateDO> selectPage(QualityTaskEvaluatePageReqVO reqVO) {
        // 定义排序的字段（防止 SQL 注入，与数据库字段名称一致）
        Set<String> allowedColumns = new HashSet<>(Arrays.asList("id", "create_time", "update_time"));

        // 构造动态查询条件
        return selectPage(reqVO, new LambdaQueryWrapperX<QualityTaskEvaluateDO>()
                .eqIfPresent(QualityTaskEvaluateDO::getTaskId, reqVO.getTaskId())
                .likeIfPresent(QualityTaskEvaluateDO::getName, reqVO.getName())
                .eqIfPresent(QualityTaskEvaluateDO::getRuleCode, reqVO.getRuleCode())
                .likeIfPresent(QualityTaskEvaluateDO::getRuleName, reqVO.getRuleName())
                .eqIfPresent(QualityTaskEvaluateDO::getWarningLevel, reqVO.getWarningLevel())
                .eqIfPresent(QualityTaskEvaluateDO::getStatus, reqVO.getStatus())
                .eqIfPresent(QualityTaskEvaluateDO::getRuleDescription, reqVO.getRuleDescription())
                .eqIfPresent(QualityTaskEvaluateDO::getErrDescription, reqVO.getErrDescription())
                .eqIfPresent(QualityTaskEvaluateDO::getSuggestion, reqVO.getSuggestion())
                .eqIfPresent(QualityTaskEvaluateDO::getWhereClause, reqVO.getWhereClause())
                .eqIfPresent(QualityTaskEvaluateDO::getObjId, reqVO.getObjId())
                .likeIfPresent(QualityTaskEvaluateDO::getObjName, reqVO.getObjName())
                .likeIfPresent(QualityTaskEvaluateDO::getTableName, reqVO.getTableName())
                .eqIfPresent(QualityTaskEvaluateDO::getEvaColumn, reqVO.getEvaColumn())
                .eqIfPresent(QualityTaskEvaluateDO::getRule, reqVO.getRule())
                .eqIfPresent(QualityTaskEvaluateDO::getCreateTime, reqVO.getCreateTime())
                // 如果 reqVO.getName() 不为空，则添加 name 的精确匹配条件（name = '<name>'）
                // .likeIfPresent(QualityTaskEvaluateDO::getName, reqVO.getName())
                // 按照 createTime 字段降序排序
                .orderBy(reqVO.getOrderByColumn(), reqVO.getIsAsc(), allowedColumns));
    }
}
