package com.datamaster.module.ontology.service.impl;

import com.datamaster.module.ontology.api.IActionTriggerApiService;
import com.datamaster.module.ontology.api.dto.ActionDataArrivalTriggerDTO;
import com.datamaster.module.ontology.controller.admin.action.vo.ExecutionRespVO;
import com.datamaster.module.ontology.service.IActionExecutionService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * 聚合部署内的数据到达动作触发入口。
 */
@Service
public class ActionTriggerApiServiceImpl implements IActionTriggerApiService {

    @Resource
    private IActionExecutionService executionService;

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
        return ids;
    }
}
