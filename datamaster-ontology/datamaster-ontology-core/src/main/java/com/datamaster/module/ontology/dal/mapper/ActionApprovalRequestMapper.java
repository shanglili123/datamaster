package com.datamaster.module.ontology.dal.mapper;

import com.datamaster.module.ontology.dal.dataobject.ActionApprovalRequestDO;
import com.datamaster.mybatis.core.mapper.BaseMapperX;
import com.datamaster.mybatis.core.query.LambdaQueryWrapperX;

import java.util.List;

/**
 * 审批请求 Mapper
 */
public interface ActionApprovalRequestMapper extends BaseMapperX<ActionApprovalRequestDO> {

    default List<ActionApprovalRequestDO> selectByExecutionId(Long actionExecutionId) {
        return selectList(new LambdaQueryWrapperX<ActionApprovalRequestDO>()
                .eq(ActionApprovalRequestDO::getActionExecutionId, actionExecutionId)
                .orderByAsc(ActionApprovalRequestDO::getId));
    }

    default ActionApprovalRequestDO selectActiveByExecutionId(Long actionExecutionId) {
        return selectOne(new LambdaQueryWrapperX<ActionApprovalRequestDO>()
                .eq(ActionApprovalRequestDO::getActionExecutionId, actionExecutionId)
                .in(ActionApprovalRequestDO::getStatus, "PENDING", "APPROVED")
                .orderByDesc(ActionApprovalRequestDO::getId)
                .last("LIMIT 1"));
    }
}