package com.datamaster.module.ontology.dal.mapper;

import com.datamaster.module.ontology.dal.dataobject.ActionWorkflowEdgeDO;
import com.datamaster.mybatis.core.mapper.BaseMapperX;
import com.datamaster.mybatis.core.query.LambdaQueryWrapperX;
import java.util.List;

public interface ActionWorkflowEdgeMapper extends BaseMapperX<ActionWorkflowEdgeDO> {
    default List<ActionWorkflowEdgeDO> selectByWorkflowId(Long workflowId) {
        return selectList(new LambdaQueryWrapperX<ActionWorkflowEdgeDO>()
                .eq(ActionWorkflowEdgeDO::getWorkflowId, workflowId)
                .orderByAsc(ActionWorkflowEdgeDO::getPriority).orderByAsc(ActionWorkflowEdgeDO::getId));
    }
    default void deleteByWorkflowId(Long workflowId) {
        delete(new LambdaQueryWrapperX<ActionWorkflowEdgeDO>().eq(ActionWorkflowEdgeDO::getWorkflowId, workflowId));
    }
}
