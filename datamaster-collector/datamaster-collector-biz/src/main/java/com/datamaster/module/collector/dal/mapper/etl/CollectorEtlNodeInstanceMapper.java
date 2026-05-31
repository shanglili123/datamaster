

package com.datamaster.module.collector.dal.mapper.etl;

import com.github.yulichang.wrapper.MPJLambdaWrapper;
import org.apache.commons.lang3.StringUtils;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.common.enums.Flag;
import com.datamaster.module.collector.controller.admin.etl.vo.CollectorEtlNodeInstancePageReqVO;
import com.datamaster.module.collector.dal.dataobject.etl.CollectorEtlNodeInstanceDO;
import com.datamaster.module.collector.dal.dataobject.etl.CollectorEtlTaskInstanceDO;
import com.datamaster.mybatis.config.MasterDataSourceConfig;
import com.datamaster.mybatis.core.mapper.BaseMapperX;

/**
 * 数据集成节点实例Mapper接口
 *
 * @author DATAMASTER
 * @date 2025-02-13
 */
public interface CollectorEtlNodeInstanceMapper extends BaseMapperX<CollectorEtlNodeInstanceDO> {

    default PageResult<CollectorEtlNodeInstanceDO> selectPage(CollectorEtlNodeInstancePageReqVO reqVO) {

        MPJLambdaWrapper<CollectorEtlNodeInstanceDO> lambdaWrapper = new MPJLambdaWrapper();

        lambdaWrapper.selectAll(CollectorEtlNodeInstanceDO.class)
                .select(CollectorEtlTaskInstanceDO::getCommandType)
                .select("t3.NICK_NAME AS personChargeName")
//                .leftJoin("COL_ETL_TASK_INSTANCE t2 ON t.TASK_INSTANCE_ID = t2.id AND t2.DEL_FLAG = '0'")
                .innerJoin(CollectorEtlTaskInstanceDO.class, CollectorEtlTaskInstanceDO::getId, CollectorEtlNodeInstanceDO::getTaskInstanceId)
                .leftJoin("SYSTEM_USER t3 ON t1.PERSON_CHARGE = " + ("mysql".equals(MasterDataSourceConfig.getDatabaseType()) ? "CAST(t3.USER_ID AS CHAR)" : "CAST(t3.USER_ID AS VARCHAR)") + " AND t3.DEL_FLAG = '0'")
                .eq(CollectorEtlTaskInstanceDO::getSubTaskFlag, String.valueOf(Flag.NO.getCode()))
                .eq(StringUtils.isNotBlank(reqVO.getTaskType()), CollectorEtlNodeInstanceDO::getTaskType, reqVO.getTaskType())
                .likeRight(StringUtils.isNotBlank(reqVO.getCatCode()), CollectorEtlTaskInstanceDO::getCatCode, reqVO.getCatCode())
                .like(StringUtils.isNotBlank(reqVO.getName()), CollectorEtlNodeInstanceDO::getName, reqVO.getName())
                .eq(StringUtils.isNotBlank(reqVO.getNodeType()), CollectorEtlNodeInstanceDO::getNodeType, reqVO.getNodeType())
                .eq(reqVO.getNodeId() != null, CollectorEtlNodeInstanceDO::getNodeId, reqVO.getNodeId())
                .eq(StringUtils.isNotBlank(reqVO.getNodeCode()), CollectorEtlNodeInstanceDO::getNodeCode, reqVO.getNodeCode())
                .eq(reqVO.getTaskInstanceId() != null, CollectorEtlNodeInstanceDO::getTaskInstanceId, reqVO.getTaskInstanceId())
                .like(StringUtils.isNotBlank(reqVO.getTaskInstanceName()), CollectorEtlNodeInstanceDO::getTaskInstanceName, reqVO.getTaskInstanceName())
                .like(StringUtils.isNotBlank(reqVO.getJobName()), CollectorEtlNodeInstanceDO::getTaskInstanceName, reqVO.getJobName())
                .eq(reqVO.getProjectId() != null, CollectorEtlNodeInstanceDO::getProjectId, reqVO.getProjectId())
                .eq(StringUtils.isNotBlank(reqVO.getProjectCode()), CollectorEtlNodeInstanceDO::getProjectCode, reqVO.getProjectCode())
                .gt(reqVO.getStartTime() != null, CollectorEtlNodeInstanceDO::getStartTime, reqVO.getStartTime())
                .le(reqVO.getEndTime() != null, CollectorEtlNodeInstanceDO::getEndTime, reqVO.getEndTime())
                .eq(StringUtils.isNotBlank(reqVO.getPriority()), CollectorEtlNodeInstanceDO::getPriority, reqVO.getPriority())
                .eq(StringUtils.isNotBlank(reqVO.getStatus()), CollectorEtlNodeInstanceDO::getStatus, reqVO.getStatus())
                .eq(reqVO.getDsId() != null, CollectorEtlNodeInstanceDO::getDsId, reqVO.getDsId())
                .eq(reqVO.getDsTaskInstanceId() != null, CollectorEtlNodeInstanceDO::getDsTaskInstanceId, reqVO.getDsTaskInstanceId())
                .eq(reqVO.getCreateTime() != null, CollectorEtlNodeInstanceDO::getCreateTime, reqVO.getCreateTime())
                .in(CollectorEtlNodeInstanceDO::getStatus, "1", "6", "7")
                .orderByDesc(CollectorEtlNodeInstanceDO::getStartTime);

        // 构造动态查询条件
        return selectJoinPage(reqVO, CollectorEtlNodeInstanceDO.class, lambdaWrapper);
    }
}
