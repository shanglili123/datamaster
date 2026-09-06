package com.datamaster.module.ontology.dal.mapper;

import com.datamaster.module.ontology.dal.dataobject.ActionWorkflowNodeDO;
import com.datamaster.mybatis.core.mapper.BaseMapperX;
import com.datamaster.mybatis.core.query.LambdaQueryWrapperX;
import java.util.List;

public interface ActionWorkflowNodeMapper extends BaseMapperX<ActionWorkflowNodeDO> {
    default List<ActionWorkflowNodeDO> selectByWorkflowId(Long workflowId) {
        return selectList(new LambdaQueryWrapperX<ActionWorkflowNodeDO>()
                .eq(ActionWorkflowNodeDO::getWorkflowId, workflowId)
                .orderByAsc(ActionWorkflowNodeDO::getId));
    }
    default void deleteByWorkflowId(Long workflowId) {
        delete(new LambdaQueryWrapperX<ActionWorkflowNodeDO>().eq(ActionWorkflowNodeDO::getWorkflowId, workflowId));
    }
}
