

package com.datamaster.metadata.dal.mapper.qa;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.commons.lang3.StringUtils;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.metadata.controller.qa.vo.QualityTaskPageReqVO;
import com.datamaster.metadata.dal.dataobject.qa.QualityTaskDO;
import com.datamaster.mybatis.core.mapper.BaseMapperX;
import com.datamaster.mybatis.core.query.MPJLambdaWrapperX;

import java.util.Arrays;

/**
 * 质量探查任务Mapper接口
 *
 * @author Chaos
 * @date 2025-07-21
 */
public interface QualityTaskMapper extends BaseMapperX<QualityTaskDO> {

    @Select("select SPACE_ID, SPACE_CODE from TAX_CATEGORY where CODE = #{catCode} and DEL_FLAG = '0' and CAT_TYPE = 'QUALITY'")
    QualityTaskDO selectQualityCatSpaceByCode(@Param("catCode") String catCode);

    default PageResult<QualityTaskDO> selectPage(QualityTaskPageReqVO reqVO) {
        String leftJoin = "TAX_CATEGORY t2 on t.CAT_CODE = t2.CODE AND t2.DEL_FLAG = '0' AND t2.CAT_TYPE = 'QUALITY'";

        MPJLambdaWrapperX<QualityTaskDO> lambdaWrapperX = new MPJLambdaWrapperX<>();
        lambdaWrapperX.selectAll(QualityTaskDO.class)
                .select("t2.NAME AS catName")
                .select("(SELECT COUNT(*) FROM COL_QUALITY_TASK_OBJ o WHERE o.TASK_ID = t.ID ) taskObjNum")
                .select("(SELECT COUNT(*) FROM COL_QUALITY_TASK_EVALUATE e WHERE e.TASK_ID = t.ID ) taskEvaluateNum");
        lambdaWrapperX.leftJoin(leftJoin);
        lambdaWrapperX.likeIfPresent(QualityTaskDO::getTaskName, reqVO.getTaskName())
                .eqIfPresent(QualityTaskDO::getCatCode, reqVO.getCatCode())
                .eqIfPresent(QualityTaskDO::getContact, reqVO.getContact())
                .eqIfPresent(QualityTaskDO::getAssetFlag, "0")
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
