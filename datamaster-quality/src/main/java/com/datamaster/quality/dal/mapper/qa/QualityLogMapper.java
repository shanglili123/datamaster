

package com.datamaster.quality.dal.mapper.qa;

import com.datamaster.common.core.page.PageResult;
import com.datamaster.mybatis.core.mapper.BaseMapperX;
import com.datamaster.mybatis.core.query.LambdaQueryWrapperX;
import com.datamaster.quality.controller.qa.vo.QualityLogPageReqVO;
import com.datamaster.quality.dal.dataobject.qa.QualityLogDO;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * 数据质量日志Mapper接口
 *
 * @author DATAMASTER
 * @date 2025-07-19
 */
public interface QualityLogMapper extends BaseMapperX<QualityLogDO> {

    default PageResult<QualityLogDO> selectPage(QualityLogPageReqVO reqVO) {
        // 定义排序的字段（防止 SQL 注入，与数据库字段名称一致）
        Set<String> allowedColumns = new HashSet<>(Arrays.asList("id", "create_time", "update_time"));

        // 构造动态查询条件
        return selectPage(reqVO, new LambdaQueryWrapperX<QualityLogDO>()
                .likeIfPresent(QualityLogDO::getName, reqVO.getName())
                .eqIfPresent(QualityLogDO::getSuccessFlag, reqVO.getSuccessFlag())
                .eqIfPresent(QualityLogDO::getStartTime, reqVO.getStartTime())
                .eqIfPresent(QualityLogDO::getEndTime, reqVO.getEndTime())
                .eqIfPresent(QualityLogDO::getQualityId, reqVO.getQualityId())
                .eqIfPresent(QualityLogDO::getScore, reqVO.getScore())
                .eqIfPresent(QualityLogDO::getProblemData, reqVO.getProblemData())
                .eqIfPresent(QualityLogDO::getCreateTime, reqVO.getCreateTime())
                // 如果 reqVO.getName() 不为空，则添加 name 的精确匹配条件（name = '<name>'）
                // .likeIfPresent(QualityLogDO::getName, reqVO.getName())
                // 按照 createTime 字段降序排序
                .orderBy(reqVO.getOrderByColumn(), reqVO.getIsAsc(), allowedColumns));
    }
}
