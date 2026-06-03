

package com.datamaster.quality.dal.mapper.qa;

import org.apache.commons.lang3.StringUtils;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.mybatis.core.mapper.BaseMapperX;
import com.datamaster.mybatis.core.query.MPJLambdaWrapperX;
import com.datamaster.quality.controller.qa.vo.QualityTaskPageReqVO;
import com.datamaster.quality.dal.dataobject.qa.QualityTaskDO;

import java.util.Arrays;

/**
 * 数据质量任务Mapper接口
 *
 * @author Chaos
 * @date 2025-07-21
 */
public interface QualityTaskMapper extends BaseMapperX<QualityTaskDO> {

    default PageResult<QualityTaskDO> selectPage(QualityTaskPageReqVO reqVO) {
        MPJLambdaWrapperX<QualityTaskDO> lambdaWrapperX = new MPJLambdaWrapperX<>();
        lambdaWrapperX.selectAll(QualityTaskDO.class)
                .select("(SELECT COUNT (*) FROM COL_QUALITY_TASK_OBJ o WHERE o.TASK_ID = t.ID ) taskObjNum")
                .select("(SELECT COUNT (*) FROM COL_QUALITY_TASK_EVALUATE e WHERE e.TASK_ID = t.ID ) taskEvaluateNum");
        lambdaWrapperX.likeIfPresent(QualityTaskDO::getTaskName, reqVO.getTaskName())
                .eqIfPresent(QualityTaskDO::getCatCode, reqVO.getCatCode())
                .eqIfPresent(QualityTaskDO::getContact, reqVO.getContact())
                .eqIfPresent(QualityTaskDO::getContactId, reqVO.getContactId())
                .eqIfPresent(QualityTaskDO::getContactNumber, reqVO.getContactNumber())
                .eqIfPresent(QualityTaskDO::getStatus, reqVO.getStatus())
                .eqIfPresent(QualityTaskDO::getDescription, reqVO.getDescription())
                .eqIfPresent(QualityTaskDO::getPriority, reqVO.getPriority())
                .eqIfPresent(QualityTaskDO::getWorkerGroup, reqVO.getWorkerGroup())
                .eqIfPresent(QualityTaskDO::getRetryTimes, reqVO.getRetryTimes())
                .eqIfPresent(QualityTaskDO::getRetryInterval, reqVO.getRetryInterval())
                .eqIfPresent(QualityTaskDO::getDelayTime, reqVO.getDelayTime())
                .eqIfPresent(QualityTaskDO::getCreateTime, reqVO.getCreateTime())
                // 如果 reqVO.getName() 不为空，则添加 name 的精确匹配条件（name = '<name>'）
                // .likeIfPresent(QualityTaskDO::getName, reqVO.getName())
                // 按照 createTime 字段降序排序
                .orderByStr(StringUtils.isNotBlank(reqVO.getOrderByColumn()), StringUtils.equals("asc", reqVO.getIsAsc()), StringUtils.isNotBlank(reqVO.getOrderByColumn()) ? Arrays.asList(reqVO.getOrderByColumn().split(",")) : null);

        // 构造动态查询条件
        return selectPage(reqVO, lambdaWrapperX);
    }
}
