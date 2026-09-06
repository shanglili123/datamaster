package com.datamaster.module.ontology.dal.mapper;

import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.ontology.controller.admin.workflow.vo.ActionWorkflowPageReqVO;
import com.datamaster.module.ontology.dal.dataobject.ActionWorkflowDO;
import com.datamaster.mybatis.core.mapper.BaseMapperX;
import com.datamaster.mybatis.core.query.LambdaQueryWrapperX;

public interface ActionWorkflowMapper extends BaseMapperX<ActionWorkflowDO> {
    default PageResult<ActionWorkflowDO> selectPage(ActionWorkflowPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<ActionWorkflowDO>()
                .eq(reqVO.getOntologyId() != null, ActionWorkflowDO::getOntologyId, reqVO.getOntologyId())
                .eq(reqVO.getStatus() != null, ActionWorkflowDO::getStatus, reqVO.getStatus())
                .like(reqVO.getName() != null, ActionWorkflowDO::getName, reqVO.getName())
                .orderByDesc(ActionWorkflowDO::getId));
    }

    default ActionWorkflowDO selectByCode(Long ontologyId, String code) {
        return selectOne(new LambdaQueryWrapperX<ActionWorkflowDO>()
                .eq(ActionWorkflowDO::getOntologyId, ontologyId)
                .eq(ActionWorkflowDO::getCode, code));
    }
}
