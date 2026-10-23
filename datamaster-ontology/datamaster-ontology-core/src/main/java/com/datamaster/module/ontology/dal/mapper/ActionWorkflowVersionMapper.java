package com.datamaster.module.ontology.dal.mapper;

import com.datamaster.module.ontology.dal.dataobject.ActionWorkflowVersionDO;
import com.datamaster.mybatis.core.mapper.BaseMapperX;
import com.datamaster.mybatis.core.query.LambdaQueryWrapperX;

import java.util.List;

public interface ActionWorkflowVersionMapper extends BaseMapperX<ActionWorkflowVersionDO> {
    default List<ActionWorkflowVersionDO> selectByWorkflowId(Long workflowId) {
        return selectList(new LambdaQueryWrapperX<ActionWorkflowVersionDO>()
                .eq(ActionWorkflowVersionDO::getWorkflowId, workflowId)
                .orderByDesc(ActionWorkflowVersionDO::getVersion));
    }

    default ActionWorkflowVersionDO selectByWorkflowIdAndVersion(Long workflowId, Integer version) {
        return selectOne(new LambdaQueryWrapperX<ActionWorkflowVersionDO>()
                .eq(ActionWorkflowVersionDO::getWorkflowId, workflowId)
                .eq(ActionWorkflowVersionDO::getVersion, version)
                .last("LIMIT 1"));
    }

    default void deleteByWorkflowId(Long workflowId) {
        delete(new LambdaQueryWrapperX<ActionWorkflowVersionDO>()
                .eq(ActionWorkflowVersionDO::getWorkflowId, workflowId));
    }
}
