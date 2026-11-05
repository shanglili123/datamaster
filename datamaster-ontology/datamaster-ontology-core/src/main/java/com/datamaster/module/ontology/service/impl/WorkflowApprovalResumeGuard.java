package com.datamaster.module.ontology.service.impl;

import com.datamaster.common.exception.ServiceException;
import com.datamaster.module.ontology.dal.dataobject.ActionExecutionDO;
import com.datamaster.module.ontology.dal.dataobject.ActionWorkflowRunDO;
import com.datamaster.module.ontology.dal.dataobject.ActionWorkflowStepDO;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.function.Function;

/** Read-only readiness check. Never approves, executes, retries or recreates an action. */
public final class WorkflowApprovalResumeGuard {
    private WorkflowApprovalResumeGuard() { }

    public static String token(ActionWorkflowRunDO run, List<ActionWorkflowStepDO> steps,
                               Function<Long, ActionExecutionDO> executions) {
        if (run == null || !"WAITING_APPROVAL".equals(run.getStatus())
                || Integer.valueOf(1).equals(run.getCancelRequested()))
            throw new ServiceException("Workflow 不处于可续跑的审批等待状态");
        Map<String, ActionWorkflowStepDO> latest = new TreeMap<>();
        if (steps != null) for (ActionWorkflowStepDO step : steps) {
            if (step == null || step.getId() == null || !Objects.equals(run.getId(), step.getRunId())
                    || step.getNodeKey() == null) throw new ServiceException("Workflow 步骤上下文不完整");
            ActionWorkflowStepDO previous = latest.get(step.getNodeKey());
            if (previous == null || step.getId() > previous.getId()) latest.put(step.getNodeKey(), step);
        }
        StringBuilder snapshot = new StringBuilder(String.valueOf(run.getId()));
        int waiting = 0;
        for (ActionWorkflowStepDO step : latest.values()) {
            if ("EXECUTED".equals(step.getStatus()) || "SKIPPED".equals(step.getStatus())) continue;
            if (!"WAITING_APPROVAL".equals(step.getStatus()) || step.getExecutionId() == null)
                throw new ServiceException("存在未完成、失败或需对账步骤，不能从决策入口自动重试");
            ActionExecutionDO execution = executions.apply(step.getExecutionId());
            if (execution == null || !Objects.equals(execution.getId(), step.getExecutionId())
                    || !Objects.equals(execution.getWorkflowRunId(), run.getId())
                    || !Objects.equals(execution.getWorkflowStepId(), step.getId())
                    || !Objects.equals(execution.getActionId(), step.getActionId())
                    || !Objects.equals(execution.getCreatorId(), run.getCreatorId())
                    || !"EXECUTED".equals(execution.getStatus()))
                throw new ServiceException("审批节点尚未执行成功或执行关联不一致，请先完成审批并刷新状态");
            snapshot.append(':').append(step.getId()).append(':').append(execution.getId());
            waiting++;
        }
        if (waiting == 0) throw new ServiceException("没有可核实的审批等待节点，不能续跑");
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(snapshot.toString().getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder();
            for (byte value : hash) result.append(String.format("%02x", value & 0xff));
            return result.toString();
        } catch (java.security.NoSuchAlgorithmException error) { throw new IllegalStateException(error); }
    }
}
