

package com.datamaster.quality.dal.mapper.qa;

import com.datamaster.common.core.page.PageResult;
import com.datamaster.mybatis.core.mapper.BaseMapperX;
import com.datamaster.mybatis.core.query.LambdaQueryWrapperX;
import com.datamaster.quality.controller.qa.vo.EvaluateLogPageReqVO;
import com.datamaster.quality.dal.dataobject.qa.EvaluateLogDO;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * 评测规则结果Mapper接口
 *
 * @author DATAMASTER
 * @date 2025-07-21
 */
public interface EvaluateLogMapper extends BaseMapperX<EvaluateLogDO> {

    default PageResult<EvaluateLogDO> selectPage(EvaluateLogPageReqVO reqVO) {
        // 定义排序的字段（防止 SQL 注入，与数据库字段名称一致）
        Set<String> allowedColumns = new HashSet<>(Arrays.asList("id", "create_time", "update_time"));

        // 构造动态查询条件
        return selectPage(reqVO, new LambdaQueryWrapperX<EvaluateLogDO>()
                .likeIfPresent(EvaluateLogDO::getTableName, reqVO.getTableName())
                .likeIfPresent(EvaluateLogDO::getColumnName, reqVO.getColumnName())
                .eqIfPresent(EvaluateLogDO::getRuleCode, reqVO.getRuleCode())
                .likeIfPresent(EvaluateLogDO::getRuleName, reqVO.getRuleName())
                .eqIfPresent(EvaluateLogDO::getDimensionType, reqVO.getDimensionType())
                .eqIfPresent(EvaluateLogDO::getRuleDescription, reqVO.getRuleDescription())
                .eqIfPresent(EvaluateLogDO::getTaskLogId, reqVO.getTaskLogId())
                .eqIfPresent(EvaluateLogDO::getEvaluateId, reqVO.getEvaluateId())
                .eqIfPresent(EvaluateLogDO::getTotal, reqVO.getTotal())
                .eqIfPresent(EvaluateLogDO::getProblemTotal, reqVO.getProblemTotal())
                .eqIfPresent(EvaluateLogDO::getCheckDate, reqVO.getCheckDate())
                .eqIfPresent(EvaluateLogDO::getCreateTime, reqVO.getCreateTime())
                // 如果 reqVO.getName() 不为空，则添加 name 的精确匹配条件（name = '<name>'）
                // .likeIfPresent(EvaluateLogDO::getName, reqVO.getName())
                // 按照 createTime 字段降序排序
                .orderBy(reqVO.getOrderByColumn(), reqVO.getIsAsc(), allowedColumns));
    }
}
