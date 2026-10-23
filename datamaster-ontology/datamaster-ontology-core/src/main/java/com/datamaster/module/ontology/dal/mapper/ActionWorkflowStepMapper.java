package com.datamaster.module.ontology.dal.mapper;

import com.datamaster.module.ontology.dal.dataobject.ActionWorkflowStepDO;
import com.datamaster.mybatis.core.mapper.BaseMapperX;
import com.datamaster.mybatis.core.query.LambdaQueryWrapperX;

import java.util.List;

public interface ActionWorkflowStepMapper extends BaseMapperX<ActionWorkflowStepDO> {
    default List<ActionWorkflowStepDO> selectByRunId(Long runId) {
        return selectList(new LambdaQueryWrapperX<ActionWorkflowStepDO>()
                .eq(ActionWorkflowStepDO::getRunId, runId)
                .orderByAsc(ActionWorkflowStepDO::getId));
    }
}
