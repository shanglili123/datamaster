package com.datamaster.module.ontology.service.impl;

import com.datamaster.module.ontology.dal.dataobject.ActionWorkflowDO;
import com.datamaster.module.ontology.dal.dataobject.ActionWorkflowRunDO;
import com.datamaster.module.ontology.dal.mapper.ActionWorkflowMapper;
import com.datamaster.module.system.api.message.dto.MessageSaveReqDTO;
import com.datamaster.module.system.service.ISysMessageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Workflow 终态告警通知。通知失败只记录日志，不改变 Workflow 的真实执行结果。
 */
@Slf4j
@Component
public class ActionWorkflowAlertService {

    /** 由增量 SQL 初始化的站内消息模板。 */
    private static final Long WORKFLOW_ALERT_TEMPLATE_ID = 9L;

    @Resource
    private ISysMessageService sysMessageService;

    @Resource
    private ActionWorkflowMapper workflowMapper;

    public void notifyTerminal(ActionWorkflowRunDO run, String status, String errorMessage) {
        if (run == null || run.getId() == null) return;
        try {
            Long receiverId = run.getCreatorId();
            ActionWorkflowDO workflow = run.getWorkflowId() == null ? null : workflowMapper.selectById(run.getWorkflowId());
            if (receiverId == null && workflow != null) receiverId = workflow.getCreatorId();
            if (receiverId == null) {
                log.warn("Workflow 终态告警缺少接收人，跳过站内消息 runId={} status={}", run.getId(), status);
                return;
            }
            MessageSaveReqDTO request = new MessageSaveReqDTO();
            request.setSenderId(1L);
            request.setCreatorId(1L);
            request.setCreateBy("系统");
            request.setReceiverId(receiverId);
            request.setModule(0);
            request.setEntityType(0);
            request.setEntityId(run.getId());
            request.setEntityUrl("/ont/workspace");

            Map<String, Object> data = new HashMap<>();
            data.put("workflowName", workflow == null || workflow.getName() == null ? "Workflow" : workflow.getName());
            data.put("workflowId", run.getWorkflowId() == null ? "" : run.getWorkflowId());
            data.put("runId", run.getId());
            data.put("status", status == null ? "UNKNOWN" : status);
            data.put("errorMessage", errorMessage == null ? "" : errorMessage);
            data.put("finishedTime", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
            sysMessageService.send(WORKFLOW_ALERT_TEMPLATE_ID, request, data);
        } catch (Exception e) {
            log.warn("发送 Workflow 终态告警失败，runId={} status={}", run.getId(), status, e);
        }
    }
}
