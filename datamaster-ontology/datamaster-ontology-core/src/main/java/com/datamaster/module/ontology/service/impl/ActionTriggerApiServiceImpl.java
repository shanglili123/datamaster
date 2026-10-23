package com.datamaster.module.ontology.service.impl;

import com.datamaster.module.ontology.api.IActionTriggerApiService;
import com.datamaster.module.ontology.api.dto.ActionDataArrivalTriggerDTO;
import com.datamaster.module.ontology.controller.admin.action.vo.ExecutionRespVO;
import com.datamaster.module.ontology.service.IActionExecutionService;
import com.datamaster.module.ontology.service.IActionWorkflowRuntimeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * 聚合部署内的数据到达动作触发入口。
 */
@Service
@Slf4j
public class ActionTriggerApiServiceImpl implements IActionTriggerApiService {

    @Resource
    private IActionExecutionService executionService;

    @Resource
    private IActionWorkflowRuntimeService workflowRuntimeService;

    @Override
    public List<Long> triggerDataArrival(ActionDataArrivalTriggerDTO request) {
        if (request == null || request.getTriggerRef() == null || request.getTriggerRef().trim().isEmpty()) {
            throw new IllegalArgumentException("triggerRef 不能为空");
        }
        List<ExecutionRespVO> executions = executionService.submitByTrigger(
                request.getTriggerRef(), request.getInputParams(), request.getObjectKey(),
                request.getEventId(), request.getSpaceId(), request.getSpaceCode());
        List<Long> ids = new ArrayList<>();
        for (ExecutionRespVO execution : executions) {
            if (execution != null && execution.getId() != null) {
                ids.add(execution.getId());
            }
        }
        try {
            int workflowCount = workflowRuntimeService.triggerByEvent(
                    request.getTriggerRef(), request.getInputParams(), request.getObjectKey(),
                    request.getEventId(), request.getSpaceId(), request.getSpaceCode()).size();
            if (workflowCount > 0) {
                log.info("数据到达触发 Workflow 入队完成 triggerRef={} eventId={} count={}",
                        request.getTriggerRef(), request.getEventId(), workflowCount);
            }
        } catch (Exception e) {
            // 保持历史动作触发接口的兼容性；Workflow 入队失败留痕并由运行监控发现。
            log.error("数据到达触发 Workflow 入队失败 triggerRef={} eventId={}",
                    request.getTriggerRef(), request.getEventId(), e);
        }
        return ids;
    }
}
