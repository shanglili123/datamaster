package com.datamaster.module.ontology.dal.mapper;

import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.ontology.controller.admin.action.vo.ExecutionPageReqVO;
import com.datamaster.module.ontology.dal.dataobject.ActionExecutionDO;
import com.datamaster.mybatis.core.mapper.BaseMapperX;
import com.datamaster.mybatis.core.query.LambdaQueryWrapperX;

import java.util.List;

/**
 * 动作执行记录 Mapper
 */
public interface ActionExecutionMapper extends BaseMapperX<ActionExecutionDO> {

    default PageResult<ActionExecutionDO> selectPage(ExecutionPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<ActionExecutionDO>()
                .eq(reqVO.getActionId() != null, ActionExecutionDO::getActionId, reqVO.getActionId())
                .eq(reqVO.getOntologyId() != null, ActionExecutionDO::getOntologyId, reqVO.getOntologyId())
                .eq(reqVO.getStatus() != null, ActionExecutionDO::getStatus, reqVO.getStatus())
                .orderByDesc(ActionExecutionDO::getId));
    }

    default List<ActionExecutionDO> selectByActionId(Long actionId) {
        return selectList(new LambdaQueryWrapperX<ActionExecutionDO>()
                .eq(ActionExecutionDO::getActionId, actionId)
                .orderByDesc(ActionExecutionDO::getId));
    }

    default List<ActionExecutionDO> selectPendingApprovals(Long ontologyId) {
        return selectList(new LambdaQueryWrapperX<ActionExecutionDO>()
                .eq(ActionExecutionDO::getOntologyId, ontologyId)
                .eq(ActionExecutionDO::getStatus, "PENDING_APPROVAL")
                .orderByDesc(ActionExecutionDO::getId));
    }
}
