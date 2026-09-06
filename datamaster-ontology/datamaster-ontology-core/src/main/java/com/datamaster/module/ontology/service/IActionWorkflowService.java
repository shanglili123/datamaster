package com.datamaster.module.ontology.service;

import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.ontology.controller.admin.workflow.vo.ActionWorkflowPageReqVO;
import com.datamaster.module.ontology.controller.admin.workflow.vo.ActionWorkflowRespVO;
import com.datamaster.module.ontology.controller.admin.workflow.vo.ActionWorkflowSaveReqVO;

public interface IActionWorkflowService {
    Long createWorkflow(ActionWorkflowSaveReqVO reqVO);
    Integer updateWorkflow(ActionWorkflowSaveReqVO reqVO);
    Integer deleteWorkflow(Long id);
    ActionWorkflowRespVO getWorkflow(Long id);
    PageResult<ActionWorkflowRespVO> getWorkflowPage(ActionWorkflowPageReqVO reqVO);
    void validateWorkflow(Long id);
    void publishWorkflow(Long id);
    void disableWorkflow(Long id);
}
