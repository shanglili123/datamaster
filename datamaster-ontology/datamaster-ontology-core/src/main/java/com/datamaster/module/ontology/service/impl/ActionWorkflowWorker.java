package com.datamaster.module.ontology.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.datamaster.module.ontology.dal.dataobject.ActionExecutionDO;
import com.datamaster.module.ontology.dal.dataobject.ActionWorkflowRunDO;
import com.datamaster.module.ontology.dal.dataobject.ActionWorkflowNodeDO;
import com.datamaster.module.ontology.dal.dataobject.ActionWorkflowStepDO;
import com.datamaster.module.ontology.dal.mapper.ActionExecutionMapper;
import com.datamaster.module.ontology.dal.mapper.ActionWorkflowNodeMapper;
import com.datamaster.module.ontology.dal.mapper.ActionWorkflowRunMapper;
import com.datamaster.module.ontology.dal.mapper.ActionWorkflowStepMapper;
import com.datamaster.module.ontology.service.IActionWorkflowRuntimeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Workflow 异步运行 Worker：以 ONT_ACTION_WORKFLOW_RUN 的 QUEUED 状态作为持久队列。
 */
@Slf4j
@Component
public class ActionWorkflowWorker {

    @Resource
    private ActionWorkflowRunMapper runMapper;

    @Resource
    private ActionWorkflowNodeMapper nodeMapper;

    @Resource
    private ActionWorkflowStepMapper stepMapper;

    @Resource
    private ActionExecutionMapper executionMapper;

    @Resource
    private IActionWorkflowRuntimeService runtimeService;

    @Resource
    private ActionWorkflowAlertService workflowAlertService;

    @Resource(name = "threadPoolTaskExecutor")
    private ThreadPoolTaskExecutor taskExecutor;

    @Value("${datamaster.ontology.workflow.worker-enabled:true}")
    private boolean workerEnabled;

    @Value("${datamaster.ontology.workflow.worker-batch-size:10}")
    private int batchSize;

    @Value("${datamaster.ontology.workflow.worker-stale-timeout-ms:300000}")
    private long staleTimeoutMs;

    private final Set<Long> submitted = ConcurrentHashMap.newKeySet();

    @Scheduled(fixedDelayString = "${datamaster.ontology.workflow.worker-delay-ms:1000}")
    public void scan() {
        // 即使关闭异步队列，也要保留节点超时监控，保证同步 Workflow 不会永久卡在 RUNNING。
        monitorNodeTimeouts();
        if (!workerEnabled) return;
        // 本实例正在执行的运行持续刷新租约；其它实例只回收超过阈值且无心跳的 RUNNING 记录。
        runMapper.touchRunning(submitted, "workflow-worker");
        long timeout = Math.max(staleTimeoutMs, 60000L);
        int recovered = runMapper.requeueStaleRunning(new Date(System.currentTimeMillis() - timeout));
        if (recovered > 0) {
            log.warn("恢复陈旧 Workflow 运行到队列 recovered={} staleTimeoutMs={}", recovered, timeout);
        }
        List<ActionWorkflowRunDO> queued = runMapper.selectQueued(batchSize);
        for (ActionWorkflowRunDO run : queued) {
            Long runId = run.getId();
            if (!submitted.add(runId)) continue;
            if (runMapper.claimQueued(runId, "workflow-worker") == 0) {
                submitted.remove(runId);
                continue;
            }
            try {
                taskExecutor.submit(() -> {
                    try {
                        runtimeService.executeQueued(runId);
                    } finally {
                        submitted.remove(runId);
                    }
                });
            } catch (Exception e) {
                runMapper.requeueRunning(runId);
                submitted.remove(runId);
                log.warn("Workflow 异步运行提交线程池失败 runId={}", runId, e);
            }
        }
    }

    /**
     * 节点超时采用“人工对账”而不是线程中断：动作可能已经写入目标系统，
     * 继续自动重试会放大副作用。底层动作和 Workflow 运行都使用条件更新，避免覆盖已完成状态。
     */
    private void monitorNodeTimeouts() {
        List<ActionWorkflowRunDO> running = runMapper.selectRunning(Math.max(batchSize, 10));
        Date now = new Date();
        for (ActionWorkflowRunDO run : running) {
            if (run == null || run.getWorkflowId() == null || run.getCurrentNodeKey() == null) continue;
            ActionWorkflowNodeDO node = nodeMapper.selectByWorkflowId(run.getWorkflowId()).stream()
                    .filter(item -> run.getCurrentNodeKey().equals(item.getNodeKey()))
                    .findFirst().orElse(null);
            if (node == null || node.getTimeoutMs() == null || node.getTimeoutMs() <= 0) continue;
            ActionWorkflowStepDO step = stepMapper.selectByRunId(run.getId()).stream()
                    .filter(item -> run.getCurrentNodeKey().equals(item.getNodeKey()))
                    .reduce((first, second) -> second).orElse(null);
            if (step == null || !"RUNNING".equals(step.getStatus()) || step.getStartedTime() == null) continue;
            long elapsed = now.getTime() - step.getStartedTime().getTime();
            if (elapsed < node.getTimeoutMs()) continue;
            String message = "Workflow 节点执行超过超时时间 " + node.getTimeoutMs()
                    + "ms，底层动作可能已产生副作用，已转人工对账";
            if (step.getExecutionId() != null) {
                ActionExecutionDO execution = executionMapper.selectById(step.getExecutionId());
                if (execution != null && ("EXECUTED".equals(execution.getStatus())
                        || "FAILED".equals(execution.getStatus())
                        || "REJECTED".equals(execution.getStatus())
                        || "RECONCILIATION_REQUIRED".equals(execution.getStatus()))) {
                    // 动作已经有确定终态，交给运行线程/恢复逻辑收敛，不能把成功动作改成超时。
                    continue;
                }
                int executionUpdated = executionMapper.markReconciliationRequired(step.getExecutionId(), message);
                if (executionUpdated == 0) {
                    ActionExecutionDO latest = executionMapper.selectById(step.getExecutionId());
                    if (latest != null && isTerminalExecution(latest.getStatus())) {
                        // 动作在监控读取和条件更新之间已经收敛，不能误报为超时。
                        continue;
                    }
                }
            }
            int stepUpdated = stepMapper.update(null, new LambdaUpdateWrapper<ActionWorkflowStepDO>()
                    .eq(ActionWorkflowStepDO::getId, step.getId())
                    .eq(ActionWorkflowStepDO::getStatus, "RUNNING")
                    .set(ActionWorkflowStepDO::getStatus, "RECONCILIATION_REQUIRED")
                    .set(ActionWorkflowStepDO::getErrorMessage, message)
                    .set(ActionWorkflowStepDO::getFinishedTime, now)
                    .set(ActionWorkflowStepDO::getUpdateTime, now));
            if (stepUpdated == 0) continue;
            int runUpdated = runMapper.update(null, new LambdaUpdateWrapper<ActionWorkflowRunDO>()
                    .eq(ActionWorkflowRunDO::getId, run.getId())
                    .eq(ActionWorkflowRunDO::getStatus, "RUNNING")
                    .eq(ActionWorkflowRunDO::getCancelRequested, 0)
                    .set(ActionWorkflowRunDO::getStatus, "RECONCILIATION_REQUIRED")
                    .set(ActionWorkflowRunDO::getErrorMessage, message)
                    .set(ActionWorkflowRunDO::getFinishedTime, now)
                    .set(ActionWorkflowRunDO::getUpdateTime, now));
            if (runUpdated > 0) {
                log.error("Workflow 节点超时，已转人工对账 runId={} nodeKey={} elapsedMs={} timeoutMs={}",
                        run.getId(), run.getCurrentNodeKey(), elapsed, node.getTimeoutMs());
                workflowAlertService.notifyTerminal(run, "RECONCILIATION_REQUIRED", message);
            }
        }
    }

    private boolean isTerminalExecution(String status) {
        return "EXECUTED".equals(status) || "FAILED".equals(status)
                || "REJECTED".equals(status) || "RECONCILIATION_REQUIRED".equals(status);
    }
}
