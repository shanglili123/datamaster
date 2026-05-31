

package com.datamaster.quality.dal.mapper.qa;

import com.datamaster.common.core.page.PageResult;
import com.datamaster.mybatis.core.mapper.BaseMapperX;
import com.datamaster.mybatis.core.query.LambdaQueryWrapperX;
import com.datamaster.quality.controller.qa.vo.DppEvaluateLogPageReqVO;
import com.datamaster.quality.dal.dataobject.qa.DppEvaluateLogDO;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * 评测规则结果Mapper接口
 *
 * @author DATAMASTER
 * @date 2025-07-21
 */
public interface DppEvaluateLogMapper extends BaseMapperX<DppEvaluateLogDO> {

    default PageResult<DppEvaluateLogDO> selectPage(DppEvaluateLogPageReqVO reqVO) {
        // 定义排序的字段（防止 SQL 注入，与数据库字段名称一致）
        Set<String> allowedColumns = new HashSet<>(Arrays.asList("id", "create_time", "update_time"));

        // 构造动态查询条件
        return selectPage(reqVO, new LambdaQueryWrapperX<DppEvaluateLogDO>()
                .likeIfPresent(DppEvaluateLogDO::getTableName, reqVO.getTableName())
                .likeIfPresent(DppEvaluateLogDO::getColumnName, reqVO.getColumnName())
                .eqIfPresent(DppEvaluateLogDO::getRuleCode, reqVO.getRuleCode())
                .likeIfPresent(DppEvaluateLogDO::getRuleName, reqVO.getRuleName())
                .eqIfPresent(DppEvaluateLogDO::getDimensionType, reqVO.getDimensionType())
                .eqIfPresent(DppEvaluateLogDO::getRuleDescription, reqVO.getRuleDescription())
                .eqIfPresent(DppEvaluateLogDO::getTaskLogId, reqVO.getTaskLogId())
                .eqIfPresent(DppEvaluateLogDO::getEvaluateId, reqVO.getEvaluateId())
                .eqIfPresent(DppEvaluateLogDO::getTotal, reqVO.getTotal())
                .eqIfPresent(DppEvaluateLogDO::getProblemTotal, reqVO.getProblemTotal())
                .eqIfPresent(DppEvaluateLogDO::getCheckDate, reqVO.getCheckDate())
                .eqIfPresent(DppEvaluateLogDO::getCreateTime, reqVO.getCreateTime())
                // 如果 reqVO.getName() 不为空，则添加 name 的精确匹配条件（name = '<name>'）
                // .likeIfPresent(DppEvaluateLogDO::getName, reqVO.getName())
                // 按照 createTime 字段降序排序
                .orderBy(reqVO.getOrderByColumn(), reqVO.getIsAsc(), allowedColumns));
    }
}
