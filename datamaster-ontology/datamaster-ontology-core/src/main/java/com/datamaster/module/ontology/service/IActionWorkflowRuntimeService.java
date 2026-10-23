package com.datamaster.module.ontology.service;

import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.ontology.controller.admin.workflow.vo.ActionWorkflowRunPageReqVO;
import com.datamaster.module.ontology.controller.admin.workflow.vo.ActionWorkflowRunReqVO;
import com.datamaster.module.ontology.controller.admin.workflow.vo.ActionWorkflowRunRespVO;
import com.datamaster.module.ontology.controller.admin.workflow.vo.ActionWorkflowMonitorRespVO;
import com.datamaster.module.ontology.controller.admin.workflow.vo.ActionWorkflowCompensationRespVO;

import java.util.List;

public interface IActionWorkflowRuntimeService {
    ActionWorkflowRunRespVO start(Long workflowId, ActionWorkflowRunReqVO reqVO);

    /**
     * 由受信任的数据到达入口触发已发布 Workflow。该入口不依赖当前登录会话，
     * 但只匹配已启用定义并校验事件携带的空间上下文。
     */
    java.util.List<ActionWorkflowRunRespVO> triggerByEvent(String triggerRef, String inputParams,
                                                            String objectKey, String eventId,
                                                            Long spaceId, String spaceCode);
    void executeQueued(Long runId);
    ActionWorkflowRunRespVO resume(Long runId, ActionWorkflowRunReqVO reqVO);
    ActionWorkflowCompensationRespVO compensate(Long runId);
    ActionWorkflowCompensationRespVO retryCompensation(Long runId);
    ActionWorkflowRunRespVO get(Long runId);
    List<ActionWorkflowRunRespVO> list(Long workflowId);
    PageResult<ActionWorkflowRunRespVO> page(Long workflowId, ActionWorkflowRunPageReqVO reqVO);
    ActionWorkflowMonitorRespVO monitor(Integer limit);
    ActionWorkflowRunRespVO cancel(Long runId);
}
