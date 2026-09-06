package com.datamaster.module.ontology.dal.mapper;

import com.datamaster.module.ontology.dal.dataobject.ActionApprovalTaskDO;
import com.datamaster.mybatis.core.mapper.BaseMapperX;
import com.datamaster.mybatis.core.query.LambdaQueryWrapperX;

import java.util.List;

/**
 * 审批关卡任务 Mapper
 */
public interface ActionApprovalTaskMapper extends BaseMapperX<ActionApprovalTaskDO> {

    default List<ActionApprovalTaskDO> selectByRequestId(Long requestId) {
        return selectList(new LambdaQueryWrapperX<ActionApprovalTaskDO>()
                .eq(ActionApprovalTaskDO::getRequestId, requestId)
                .orderByAsc(ActionApprovalTaskDO::getStageNo));
    }

    default ActionApprovalTaskDO selectCurrentPending(Long requestId, Integer stageNo) {
        return selectOne(new LambdaQueryWrapperX<ActionApprovalTaskDO>()
                .eq(ActionApprovalTaskDO::getRequestId, requestId)
                .eq(ActionApprovalTaskDO::getStageNo, stageNo)
                .eq(ActionApprovalTaskDO::getStatus, "PENDING")
                .last("LIMIT 1"));
    }
}