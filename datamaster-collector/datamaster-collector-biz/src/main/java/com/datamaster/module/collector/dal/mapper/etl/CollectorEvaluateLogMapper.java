

package com.datamaster.module.collector.dal.mapper.etl;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import com.datamaster.module.collector.controller.admin.etl.vo.CollectorEvaluateLogStatisticsVO;
import com.datamaster.module.collector.dal.dataobject.etl.CollectorEvaluateLogDO;
import java.util.Arrays;
import com.datamaster.common.core.page.PageResult;
import java.util.*;
import com.datamaster.module.collector.controller.admin.etl.vo.CollectorEvaluateLogPageReqVO;
import com.datamaster.mybatis.core.mapper.BaseMapperX;
import com.datamaster.mybatis.core.query.LambdaQueryWrapperX;

/**
 * 评测规则结果Mapper接口
 *
 * @author DATAMASTER
 * @date 2025-07-21
 */
public interface CollectorEvaluateLogMapper extends BaseMapperX<CollectorEvaluateLogDO> {

    default PageResult<CollectorEvaluateLogDO> selectPage(CollectorEvaluateLogPageReqVO reqVO) {
        // 定义排序的字段（防止 SQL 注入，与数据库字段名称一致）
        Set<String> allowedColumns = new HashSet<>(Arrays.asList("id", "create_time", "update_time"));

        // 构造动态查询条件
        return selectPage(reqVO, new LambdaQueryWrapperX<CollectorEvaluateLogDO>()
                .likeIfPresent(CollectorEvaluateLogDO::getTableName, reqVO.getTableName())
                .likeIfPresent(CollectorEvaluateLogDO::getColumnName, reqVO.getColumnName())
                .eqIfPresent(CollectorEvaluateLogDO::getRuleCode, reqVO.getRuleCode())
                .likeIfPresent(CollectorEvaluateLogDO::getRuleName, reqVO.getRuleName())
                .eqIfPresent(CollectorEvaluateLogDO::getDimensionType, reqVO.getDimensionType())
                .eqIfPresent(CollectorEvaluateLogDO::getRuleDescription, reqVO.getRuleDescription())
                .eqIfPresent(CollectorEvaluateLogDO::getTaskLogId, reqVO.getTaskLogId())
                .eqIfPresent(CollectorEvaluateLogDO::getEvaluateId, reqVO.getEvaluateId())
                .eqIfPresent(CollectorEvaluateLogDO::getTotal, reqVO.getTotal())
                .eqIfPresent(CollectorEvaluateLogDO::getProblemTotal, reqVO.getProblemTotal())
                .eqIfPresent(CollectorEvaluateLogDO::getCheckDate, reqVO.getCheckDate())
                .eqIfPresent(CollectorEvaluateLogDO::getCreateTime, reqVO.getCreateTime())
                // 如果 reqVO.getName() 不为空，则添加 name 的精确匹配条件（name = '<name>'）
                // .likeIfPresent(CollectorEvaluateLogDO::getName, reqVO.getName())
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
    List<CollectorEvaluateLogStatisticsVO> selectDimStatsByTaskLogId(@Param("taskLogId") Long taskLogId);

    List<Map<String, Object>> getEvaluateTrend7d();

}
