

package com.datamaster.module.collector.dal.mapper.qa;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.commons.lang3.StringUtils;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.collector.controller.admin.qa.vo.CollectorQualityTaskPageReqVO;
import com.datamaster.module.collector.dal.dataobject.qa.CollectorQualityTaskDO;
import com.datamaster.mybatis.core.mapper.BaseMapperX;
import com.datamaster.mybatis.core.query.MPJLambdaWrapperX;

import java.util.Arrays;

/**
 * 数据质量任务Mapper接口
 *
 * @author Chaos
 * @date 2025-07-21
 */
public interface CollectorQualityTaskMapper extends BaseMapperX<CollectorQualityTaskDO> {

    @Select("select PROJECT_ID, PROJECT_CODE from TAX_QUALITY_CAT where CODE = #{catCode} and DEL_FLAG = '0'")
    CollectorQualityTaskDO selectQualityCatProjectByCode(@Param("catCode") String catCode);

    default PageResult<CollectorQualityTaskDO> selectPage(CollectorQualityTaskPageReqVO reqVO) {
        String leftJoin = "TAX_QUALITY_CAT t2 on t.CAT_CODE = t2.CODE AND t2.DEL_FLAG = '0'";

        MPJLambdaWrapperX<CollectorQualityTaskDO> lambdaWrapperX = new MPJLambdaWrapperX<>();
        lambdaWrapperX.selectAll(CollectorQualityTaskDO.class)
                .select("t2.NAME AS catName")
                .select("(SELECT COUNT(*) FROM COL_QUALITY_TASK_OBJ o WHERE o.TASK_ID = t.ID ) taskObjNum")
                .select("(SELECT COUNT(*) FROM COL_QUALITY_TASK_EVALUATE e WHERE e.TASK_ID = t.ID ) taskEvaluateNum");
        lambdaWrapperX.leftJoin(leftJoin);
        lambdaWrapperX.likeIfPresent(CollectorQualityTaskDO::getTaskName, reqVO.getTaskName())
                .eqIfPresent(CollectorQualityTaskDO::getCatCode, reqVO.getCatCode())
                .eqIfPresent(CollectorQualityTaskDO::getContact, reqVO.getContact())
                .eqIfPresent(CollectorQualityTaskDO::getAssetFlag, "0")
                .eqIfPresent(CollectorQualityTaskDO::getContactId, reqVO.getContactId())
                .eqIfPresent(CollectorQualityTaskDO::getContactNumber, reqVO.getContactNumber())
                .eqIfPresent(CollectorQualityTaskDO::getStatus, reqVO.getStatus())
                .eqIfPresent(CollectorQualityTaskDO::getDescription, reqVO.getDescription())
                .eqIfPresent(CollectorQualityTaskDO::getPriority, reqVO.getPriority())
                .eqIfPresent(CollectorQualityTaskDO::getWorkerGroup, reqVO.getWorkerGroup())
                .eqIfPresent(CollectorQualityTaskDO::getRetryTimes, reqVO.getRetryTimes())
                .eqIfPresent(CollectorQualityTaskDO::getRetryInterval, reqVO.getRetryInterval())
                .eqIfPresent(CollectorQualityTaskDO::getDelayTime, reqVO.getDelayTime())
                .eqIfPresent(CollectorQualityTaskDO::getCreateTime, reqVO.getCreateTime())
                // 如果 reqVO.getName() 不为空，则添加 name 的精确匹配条件（name = '<name>'）
                // .likeIfPresent(CollectorQualityTaskDO::getName, reqVO.getName())
                // 按照 createTime 字段降序排序
                .orderByStr(StringUtils.isNotBlank(reqVO.getOrderByColumn()), StringUtils.equals("asc", reqVO.getIsAsc()), StringUtils.isNotBlank(reqVO.getOrderByColumn()) ? Arrays.asList(reqVO.getOrderByColumn().split(",")) : null);

        // 构造动态查询条件
        return selectPage(reqVO, lambdaWrapperX);
    }
}
