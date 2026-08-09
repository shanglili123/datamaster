

package com.datamaster.metadata.dal.mapper.qa;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import com.datamaster.metadata.controller.qa.vo.EvaluateLogStatisticsVO;
import com.datamaster.metadata.dal.dataobject.qa.EvaluateLogDO;
import java.util.Arrays;
import com.datamaster.common.core.page.PageResult;
import java.util.*;
import com.datamaster.metadata.controller.qa.vo.EvaluateLogPageReqVO;
import com.datamaster.mybatis.core.mapper.BaseMapperX;
import com.datamaster.mybatis.core.query.LambdaQueryWrapperX;

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


    @Select(
            "SELECT " +
                    "    t.DIMENSION_TYPE            AS dimensionType, " +
                    "    count(1)           AS succesTotal, " +
                    "    SUM(t.TOTAL)                AS total, " +
                    "    SUM(t.PROBLEM_TOTAL)        AS problemTotal, " +
                    "    CASE " +
                    "        WHEN SUM(t.TOTAL) > 0 " +
                    "        THEN ROUND(CAST(SUM(t.PROBLEM_TOTAL) AS DECIMAL(18,6)) * 100 " +
                    "                   / CAST(SUM(t.TOTAL) AS DECIMAL(18,6)), 2) " +
                    "        ELSE 0 " +
                    "    END                         AS proportion, " +
                    "    NULL                        AS trendType " +
                    "FROM COL_EVALUATE_LOG t " +
                    "WHERE t.TASK_LOG_ID = #{taskLogId} " +
                    "  AND t.DEL_FLAG = '0' " +
                    "  AND t.VALID_FLAG = '1' " +
                    "GROUP BY t.DIMENSION_TYPE " +
                    "ORDER BY t.DIMENSION_TYPE"
    )
    List<EvaluateLogStatisticsVO> selectDimStatsByTaskLogId(@Param("taskLogId") String taskLogId);

    List<Map<String, Object>> getEvaluateTrend7d();

}
