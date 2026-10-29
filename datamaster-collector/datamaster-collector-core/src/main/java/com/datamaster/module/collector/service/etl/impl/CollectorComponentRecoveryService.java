package com.datamaster.module.collector.service.etl.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.datamaster.api.ds.api.base.DsStatusRespDTO;
import com.datamaster.api.ds.api.service.etl.IDsEtlTaskService;
import com.datamaster.common.exception.ServiceException;
import com.datamaster.module.collector.dal.dataobject.etl.*;
import com.datamaster.module.collector.dal.mapper.etl.*;
import com.datamaster.module.collector.service.etl.*;
import com.datamaster.module.collector.utils.ComponentRecoverySupport;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.EventListener;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import java.util.*;

/** 新组件恢复复用运维事件；只提交完整工作流，不从失败节点跳过prepare。 */
@Slf4j
@Service
public class CollectorComponentRecoveryService {
    public static final String ACTION = "RESTART_COMPONENT_TASK";
    @Resource private CollectorEtlTaskOpsEventMapper events;
    @Resource private CollectorEtlTaskInstanceMapper instances;
    @Resource private CollectorEtlNodeInstanceMapper nodes;
    @Resource private CollectorEtlTaskMapper tasks;
    @Resource private CollectorEtlRunMapper runs;
    @Resource private ICollectorEtlTaskExtService extensions;
    @Resource private ICollectorEtlTaskOpsPolicyService policies;
    @Resource @Lazy private ICollectorEtlTaskService taskService;
    @Resource @Lazy private CollectorEtlTaskOpsServiceImpl diagnostics;
    @Resource private IDsEtlTaskService ds;
    @Resource private com.datamaster.api.ds.api.service.etl.IDsEtlSchedulerService dsSchedules;
    @Resource private ICollectorEtlSchedulerService schedules;
    @Resource private com.datamaster.common.api.space.ISpaceApi spaces;
    @Resource private com.datamaster.module.system.api.message.SystemNoticeApi notices;
    @Resource private org.springframework.transaction.PlatformTransactionManager transactionManager;

    /** 调用者在整流程收尾事务内已持有task锁。这里只登记事实，不调用模型或发起执行。 */
    @Transactional(propagation = Propagation.MANDATORY, rollbackFor = Exception.class)
    public void finished(CollectorEtlRunDO run, String result) {
        CollectorEtlTaskInstanceDO instance = instances.selectById(run.getId());
        if (instance == null) throw new ServiceException("恢复事件缺少本地执行记录");
        CollectorEtlTaskOpsEventDO current = events.latestComponent(run.getTaskId());
        if (current != null) {
            if ("STOP".equals(result) && !terminal(current.getActionStatus())
                    && (Objects.equals(recovery(current).getLong("executionId"), run.getId())
                    || (recovery(current).getLong("executionId") == null
                    && Objects.equals(recovery(current).getLong("failedExecutionId"), run.getId())))) {
                transition(current, "CANCELLED", "任务执行已人工停止，不自动拉起"); return;
            }
            JSONObject meta = recovery(current);
            if (Objects.equals(meta.getLong("executionId"), run.getId()) && "RUNNING".equals(current.getActionStatus())) {
                if ("SUCCESS".equals(result)) {
                    transition(current, "SUCCESS", "恢复执行已真实成功，窗口已提交");
                    return;
                }
                if ("STOP".equals(result)) {
                    transition(current, "CANCELLED", "执行已停止，不自动拉起");
                    return;
                }
                meta.put("failedExecutionId", run.getId());
                meta.remove("executionId");
                current.setAiRawResult(envelope(current, meta));
                transition(current, "PENDING_ANALYSIS", "恢复执行再次失败，沿用本次故障计数");
                return;
            }
            if (Objects.equals(current.getTaskInstanceId(), run.getId())) return;
            if (!terminal(current.getActionStatus())) return;
        }
        if (!"FAILURE".equals(result)) return;
        CollectorEtlTaskOpsPolicyDO policy = policies.getByTaskId(run.getTaskId());
        if (policy == null || (!Boolean.TRUE.equals(policy.getAiManaged()) && !Boolean.TRUE.equals(policy.getFailStopEnabled()))) return;
        CollectorEtlTaskExtDO ext = extensions.getByTaskId(run.getTaskId());
        CollectorEtlTaskOpsEventDO event = new CollectorEtlTaskOpsEventDO();
        event.setTaskId(run.getTaskId()); event.setTaskCode(instance.getTaskCode()); event.setTaskType(instance.getTaskType());
        event.setTaskInstanceId(run.getId()); event.setDsProcessInstanceId(instance.getDsId()); event.setInstanceStatus(instance.getStatus());
        event.setEventType("TASK_FAILED"); event.setAction(ACTION); event.setActionStatus("PENDING_ANALYSIS");
        event.setReason("工作流失败已确认，等待诊断"); event.setValidFlag(true); event.setDelFlag(false);
        JSONObject meta = new JSONObject();
        meta.put("version", 1); meta.put("attempt", 0); meta.put("failedExecutionId", run.getId());
        meta.put("workflowCode", ext.getEtlTaskCode()); meta.put("workflowVersion", ext.getEtlTaskVersion());
        event.setAiRawResult(envelope(event, meta));
        if (events.insert(event) != 1) throw new ServiceException("失败事件登记失败");
        dispatchAfterCommit(event.getId());
    }

    @Transactional(propagation = Propagation.MANDATORY, rollbackFor = Exception.class)
    public void cancelPending(Long taskId, String reason) {
        CollectorEtlTaskOpsEventDO event = events.latestComponent(taskId);
        if (event != null && !terminal(event.getActionStatus()) && !"RUNNING".equals(event.getActionStatus())) {
            transition(event, "CANCELLED", reason);
        }
    }

    /** 新的同版本手工/定时运行也可解除故障，不假定它一定由AI提交。 */
    @Transactional(propagation = Propagation.MANDATORY, rollbackFor = Exception.class)
    public void admitted(CollectorEtlRunDO run, Integer workflowVersion) {
        CollectorEtlTaskOpsEventDO event = events.latestComponent(run.getTaskId());
        if (event == null || terminal(event.getActionStatus()) || "RUNNING".equals(event.getActionStatus())) return;
        JSONObject meta = recovery(event);
        if (Objects.equals(meta.getLong("failedExecutionId"), run.getId())) return;
        if (!Objects.equals(meta.getString("workflowCode"), String.valueOf(run.getWorkflowCode()))
                || !Objects.equals(meta.getString("workflowVersion"), String.valueOf(workflowVersion))) {
            transition(event, "CANCELLED", "发布版本变化，取消原故障恢复"); return;
        }
        meta.put("executionId", run.getId());
        meta.put("associatedBy", "STARTING".equals(event.getActionStatus()) ? "SUBMISSION_OR_EXTERNAL_RUN" : "EXTERNAL_RUN");
        event.setAiRawResult(envelope(event, meta));
        transition(event, "RUNNING", "已关联同版本新执行，等待真实结果");
    }

    @Resource(name = "scheduledExecutorService")
    private java.util.concurrent.ScheduledExecutorService executor;
    private final Set<Long> scheduledEvents = java.util.concurrent.ConcurrentHashMap.newKeySet();
    private final Map<Long, java.util.concurrent.ScheduledFuture<?>> delayedEvents = new HashMap<>();

    /** 启动时仅补查一次，正常运行由事件提交触发，不定期扫描事件表。 */
    @EventListener(ApplicationReadyEvent.class)
    public void restorePendingEvents() {
        long afterId = 0;
        while (true) {
            List<CollectorEtlTaskOpsEventDO> batch;
            try { batch = events.pendingComponent(afterId); }
            catch (RuntimeException ex) { log.warn("启动补偿无法读取恢复事件，请人工核对"); return; }
            for (CollectorEtlTaskOpsEventDO event : batch) {
                afterId = event.getId();
                dispatchAfterCommit(event.getId());
            }
            if (batch.size() < 100) break;
        }
        afterId = 0;
        while (true) {
            List<CollectorEtlTaskOpsEventDO> batch;
            try { batch = events.pendingComponentNotices(afterId); }
            catch (RuntimeException ex) { log.warn("启动补偿无法读取恢复通知，请人工核对"); return; }
            for (CollectorEtlTaskOpsEventDO event : batch) {
                afterId = event.getId();
                dispatchAfterCommit(event.getId());
            }
            if (batch.size() < 100) break;
        }
    }

    private void dispatchAfterCommit(Long eventId) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCommit() {
                    try { executor.execute(() -> scheduleEvent(eventId)); }
                    catch (RuntimeException ex) { log.warn("恢复事件提交后调度失败，待启动补偿 eventId={}", eventId); }
                }
            });
        } else {
            scheduleEvent(eventId);
        }
    }

    private synchronized void scheduleEvent(Long eventId) {
        java.util.concurrent.ScheduledFuture<?> delayed = delayedEvents.remove(eventId);
        if (delayed != null && delayed.cancel(false)) scheduledEvents.remove(eventId);
        if (!scheduledEvents.add(eventId)) return;
        try {
            CollectorEtlTaskOpsEventDO event = events.selectById(eventId);
            if (event == null) { scheduledEvents.remove(eventId); return; }
            JSONObject meta = recovery(event);
            long due;
            if ("PENDING_ANALYSIS".equals(event.getActionStatus())) due = System.currentTimeMillis();
            else if ("WAITING".equals(event.getActionStatus())) due = meta.getLongValue("nextRetryAt");
            else if ("STARTING".equals(event.getActionStatus())) due = meta.getLongValue("submittedAt") + 600000L;
            else if (meta.getBooleanValue("notificationPending")) due = meta.getLongValue("noticeRetryAt");
            else { scheduledEvents.remove(eventId); return; }
            delayedEvents.put(eventId, executor.schedule(() -> handleEvent(eventId), Math.max(0L, due - System.currentTimeMillis()), java.util.concurrent.TimeUnit.MILLISECONDS));
        } catch (RuntimeException ex) {
            scheduledEvents.remove(eventId);
            log.warn("恢复事件调度失败，记录保留等待重启补偿 eventId={}", eventId);
        }
    }

    private void handleEvent(Long eventId) {
        synchronized (this) { delayedEvents.remove(eventId); }
        String before = null;
        String metadata = null;
        try {
            CollectorEtlTaskOpsEventDO event = events.selectById(eventId);
            if (event == null) return;
            before = event.getActionStatus(); metadata = event.getAiRawResult();
            if ("PENDING_ANALYSIS".equals(before)) analyze(event);
            else if ("WAITING".equals(before)) submit(event);
            else if ("STARTING".equals(before)) reconcileSubmission(event);
            else if (recovery(event).getBooleanValue("notificationPending")) sendNotices(event);
        } catch (RuntimeException ex) {
            log.warn("恢复事件处理失败，保留记录待人工核对或启动补偿 eventId={}", eventId);
        } finally {
            scheduledEvents.remove(eventId);
            try {
                CollectorEtlTaskOpsEventDO latest = events.selectById(eventId);
                if (latest != null && (!Objects.equals(before, latest.getActionStatus())
                        || !Objects.equals(metadata, latest.getAiRawResult()))) scheduleEvent(eventId);
            } catch (RuntimeException ex) { log.warn("恢复后续动作调度失败 eventId={}", eventId); }
        }
    }


    private void analyze(CollectorEtlTaskOpsEventDO selected) {
        JSONObject original = recovery(selected);
        CollectorEtlTaskDO task = tasks.selectById(selected.getTaskId());
        CollectorEtlTaskInstanceDO failed = instances.selectById(original.getLong("failedExecutionId"));
        if (task == null || failed == null) return;
        CollectorEtlTaskOpsPolicyDO beforeAnalysis = policies.getByTaskId(task.getId());
        if (!eligible(selected, beforeAnalysis)) {
            new TransactionTemplate(transactionManager).execute(tx -> {
                runs.lockTask(task.getId());
                CollectorEtlTaskOpsEventDO current = events.lockEvent(selected.getId());
                if (current != null && "PENDING_ANALYSIS".equals(current.getActionStatus()) && !eligible(current, policies.getByTaskId(task.getId()))) {
                    transition(current, "CANCELLED", "策略或发布配置已变化，取消诊断恢复");
                }
                return null;
            });
            return;
        }
        String excerpt = loadEvidence(failed);
        CollectorEtlTaskOpsPolicyDO initialPolicy = policies.getByTaskId(task.getId());
        CollectorEtlTaskOpsServiceImpl.OpsAnalysis analysis = initialPolicy != null && Boolean.TRUE.equals(initialPolicy.getAiManaged())
                && !Boolean.TRUE.equals(initialPolicy.getFailStopEnabled()) ? diagnostics.analyzeComponentFailure(task, failed, excerpt) : null;
        new TransactionTemplate(transactionManager).execute(tx -> {
            runs.lockTask(task.getId());
            CollectorEtlTaskOpsEventDO event = events.lockEvent(selected.getId());
            if (event == null || !"PENDING_ANALYSIS".equals(event.getActionStatus())
                    || !Objects.equals(recovery(event).getLong("failedExecutionId"), failed.getId())) return null;
            CollectorEtlTaskOpsPolicyDO policy = policies.getByTaskId(task.getId());
            if (!eligible(event, policy)) { transition(event, "CANCELLED", "策略、版本或任务状态已变化"); return null; }
            event.setLogExcerpt(excerpt);
            JSONObject meta = recovery(event);
            if (analysis != null) {
                event.setFailureType(analysis.getFailureType()); event.setRiskLevel(analysis.getRiskLevel());
                event.setSuggestion(ComponentRecoverySupport.sanitize(analysis.getSuggestion()));
                JSONObject data = JSON.parseObject(event.getAiRawResult());
                data.put("analysis", ComponentRecoverySupport.sanitize(analysis.getAiRawResult())); event.setAiRawResult(data.toJSONString());
            }
            if (Boolean.TRUE.equals(policy.getFailStopEnabled())) {
                meta.put("exhaustionCause", "FAIL_STOP"); event.setAiRawResult(envelope(event, meta));
                transition(event, "EXHAUSTED", "失败即停优先，等待下线任务及调度"); return null;
            }
            if (!Boolean.TRUE.equals(policy.getAutoRecoverEnabled()) || !"SAFE_AUTO".equals(policy.getRecoverStrategy())) {
                transition(event, "MANUAL", "策略为只给建议，不自动启动"); return null;
            }
            boolean safe = safeIncremental(event.getTaskId()) && ComponentRecoverySupport.transientFailure(excerpt)
                    && analysis != null && analysis.isRecoverable();
            event.setRecoverable(safe);
            if (!safe) { transition(event, "MANUAL", "未通过瞬时故障及幂等写入安全检查，请人工处理"); return null; }
            int attempt = meta.getIntValue("attempt");
            if (attempt >= budget(policy)) { transition(event, "EXHAUSTED", "本次连续故障恢复次数已用完"); return null; }
            meta.put("nextRetryAt", System.currentTimeMillis() + ComponentRecoverySupport.delayMillis(attempt + 1));
            event.setAiRawResult(envelope(event, meta));
            transition(event, "WAITING", "等待退避时间后整流程重跑，成功后结束本次故障链");
            return null;
        });
    }

    private void submit(CollectorEtlTaskOpsEventDO selected) {
        if (recovery(selected).getLongValue("nextRetryAt") > System.currentTimeMillis()) return;
        Boolean claimed = new TransactionTemplate(transactionManager).execute(tx -> {
            runs.lockTask(selected.getTaskId());
            CollectorEtlTaskOpsEventDO event = events.lockEvent(selected.getId());
            if (event == null || !"WAITING".equals(event.getActionStatus())) return false;
            CollectorEtlTaskOpsPolicyDO policy = policies.getByTaskId(event.getTaskId());
            if (!eligible(event, policy) || Boolean.TRUE.equals(policy.getFailStopEnabled())
                    || !Boolean.TRUE.equals(policy.getAutoRecoverEnabled()) || !"SAFE_AUTO".equals(policy.getRecoverStrategy())) {
                transition(event, "CANCELLED", "恢复策略或发布状态已变化"); return false;
            }
            JSONObject meta = recovery(event);
            if (meta.getLongValue("nextRetryAt") > System.currentTimeMillis()) return false;
            if (!safeIncremental(event.getTaskId())) { transition(event, "MANUAL", "执行快照不再满足安全重跑条件"); return false; }
            if (meta.getIntValue("attempt") >= budget(policy)) { transition(event, "EXHAUSTED", "本次恢复次数已用完"); return false; }
            if (!runs.findActive(event.getTaskId()).isEmpty()) return false;
            CollectorEtlTaskInstanceDO latest = instances.latestMainInstance(event.getTaskId());
            if (latest != null && !Objects.equals(latest.getId(), meta.getLong("failedExecutionId"))) {
                transition(event, "CANCELLED", "已有更新的执行记录，不再自动重复启动"); return false;
            }
            meta.put("attempt", meta.getIntValue("attempt") + 1); meta.put("submittedAt", System.currentTimeMillis());
            meta.put("beforeExecutionId", latest == null ? null : latest.getId());
            meta.put("beforeDsId", latest == null ? null : latest.getDsId());
            event.setAiRawResult(envelope(event, meta));
            transition(event, "STARTING", "正在发起完整工作流，尚未确认执行结果"); return true;
        });
        if (!Boolean.TRUE.equals(claimed)) return;
        try {
            DsStatusRespDTO response = taskService.startComponentRecovery(selected.getId());
            new TransactionTemplate(transactionManager).execute(tx -> {
                runs.lockTask(selected.getTaskId());
                CollectorEtlTaskOpsEventDO event = events.lockEvent(selected.getId());
                if (event == null || !"STARTING".equals(event.getActionStatus())) return null;
                if (response == null || (!response.isOk() && response.getCode() == null
                        && response.getSuccess() == null && !Boolean.TRUE.equals(response.getFailed()))) return null;
                if (!response.isOk() || Boolean.FALSE.equals(response.getData())) transition(event, "PENDING_ANALYSIS", "DS明确拒绝启动，将按限次策略重新判断");
                else {
                    JSONObject meta = recovery(event); meta.put("accepted", true); event.setAiRawResult(envelope(event, meta));
                    transition(event, "STARTING", "DS已接受启动请求，等待根prepare关联本地执行");
                }
                return null;
            });
        } catch (RuntimeException ex) {
            // 请求可能已经被DS接受，不能自动再次提交。
            log.warn("恢复提交结果待确认，禁止盲目重复 eventId={}", selected.getId());
        }
    }

    private void reconcileSubmission(CollectorEtlTaskOpsEventDO selected) {
        if (System.currentTimeMillis() - recovery(selected).getLongValue("submittedAt") < 600000L) return;
        new TransactionTemplate(transactionManager).execute(tx -> {
            runs.lockTask(selected.getTaskId());
            CollectorEtlTaskOpsEventDO event = events.lockEvent(selected.getId());
            if (event != null && "STARTING".equals(event.getActionStatus())) {
                CollectorEtlTaskDO task = tasks.selectById(event.getTaskId());
                JSONObject meta = recovery(event);
                JSONObject latest = ds.getLatestWorkflowInstance(task.getSpaceCode(), meta.getString("workflowCode"));
                CollectorEtlTaskInstanceDO failed = instances.selectById(meta.getLong("failedExecutionId"));
                if (failed != null && failed.getDsId() != null && latest.getLongValue("id") > failed.getDsId()
                        && !Arrays.asList("SUCCESS", "FAILURE", "STOP").contains(latest.getString("state"))) return null;
                transition(event, "MANUAL", "启动请求仍未关联执行，结果不确定；请核对DS，不自动重复提交");
            }
            return null;
        });
    }

    private String loadEvidence(CollectorEtlTaskInstanceDO failed) {
        StringBuilder text = new StringBuilder();
        List<CollectorEtlNodeInstanceDO> failedNodes = nodes.selectList(Wrappers.lambdaQuery(CollectorEtlNodeInstanceDO.class)
                .eq(CollectorEtlNodeInstanceDO::getTaskInstanceId, failed.getId()).eq(CollectorEtlNodeInstanceDO::getStatus, "6"));
        for (CollectorEtlNodeInstanceDO node : failedNodes) {
            if (node.getDsId() == null || text.length() >= 12000) continue;
            try {
                com.datamaster.api.ds.api.etl.DsLogResultDTO result = ds.getTaskInstanceLog(node.getDsId(), 0, 200);
                if (result != null && result.getLogContent() != null) text.append(result.getLogContent()).append('\n');
            } catch (RuntimeException ex) { log.warn("恢复诊断日志暂不可读取 nodeId={}", node.getId()); }
        }
        String sanitized = ComponentRecoverySupport.sanitize(text.toString());
        return sanitized.substring(0, Math.min(12000, sanitized.length()));
    }

    private boolean eligible(CollectorEtlTaskOpsEventDO event, CollectorEtlTaskOpsPolicyDO policy) {
        CollectorEtlTaskDO task = tasks.selectById(event.getTaskId());
        CollectorEtlTaskExtDO ext = extensions.getByTaskId(event.getTaskId());
        JSONObject meta = recovery(event);
        return task != null && "1".equals(task.getStatus()) && policy != null
                && (Boolean.TRUE.equals(policy.getAiManaged()) || Boolean.TRUE.equals(policy.getFailStopEnabled()))
                && ext != null && Objects.equals(ext.getEtlTaskCode(), meta.getString("workflowCode"))
                && Objects.equals(String.valueOf(ext.getEtlTaskVersion()), meta.getString("workflowVersion"));
    }

    private void sendNotices(CollectorEtlTaskOpsEventDO selected) {
        JSONObject initial = recovery(selected);
        if (initial.getLongValue("noticeRetryAt") > System.currentTimeMillis()) return;
        CollectorEtlTaskDO task = tasks.selectById(selected.getTaskId());
        CollectorEtlTaskOpsPolicyDO policy = policies.getByTaskId(selected.getTaskId());
        if (task == null || policy == null) return;
        if ("EXHAUSTED".equals(selected.getActionStatus()) && !initial.getBooleanValue("offlineAttempted")) {
            new TransactionTemplate(transactionManager).execute(offlineTx -> {
            runs.lockTask(task.getId());
            CollectorEtlTaskOpsEventDO fresh = events.lockEvent(selected.getId());
            CollectorEtlTaskExtDO currentExt = extensions.getByTaskId(task.getId());
            CollectorEtlTaskInstanceDO newest = instances.latestMainInstance(task.getId());
            JSONObject latestMeta = fresh == null ? new JSONObject() : recovery(fresh);
            if (fresh == null || !"EXHAUSTED".equals(fresh.getActionStatus()) || latestMeta.getBooleanValue("offlineAttempted")) return null;
            CollectorEtlTaskOpsPolicyDO currentPolicy = policies.getByTaskId(task.getId());
            boolean failStop = "FAIL_STOP".equals(latestMeta.getString("exhaustionCause"));
            if (currentPolicy == null || (failStop ? !Boolean.TRUE.equals(currentPolicy.getFailStopEnabled())
                    : !Boolean.TRUE.equals(currentPolicy.getAiManaged()) || !Boolean.TRUE.equals(currentPolicy.getAutoRecoverEnabled())
                    || !"SAFE_AUTO".equals(currentPolicy.getRecoverStrategy()) || latestMeta.getIntValue("attempt") < budget(currentPolicy))) {
                transition(fresh, "CANCELLED", "用户已修改恢复策略，取消旧故障下线动作"); return null;
            }
            if (currentExt == null || !Objects.equals(currentExt.getEtlTaskCode(), latestMeta.getString("workflowCode"))
                    || !Objects.equals(String.valueOf(currentExt.getEtlTaskVersion()), latestMeta.getString("workflowVersion"))
                    || !runs.findActive(task.getId()).isEmpty()
                    || (newest != null && !Objects.equals(newest.getId(), latestMeta.getLong("failedExecutionId")))) {
                transition(fresh, "CANCELLED", "已有新版本或新执行，取消旧故障下线动作"); return null;
            }
            boolean offline = true;
            try {
                DsStatusRespDTO response = ds.releaseTask("OFFLINE", task.getSpaceCode(), initial.getString("workflowCode"));
                offline = response != null && response.isOk() && !Boolean.FALSE.equals(response.getData());
                CollectorEtlSchedulerDO schedule = schedules.getOne(Wrappers.lambdaQuery(CollectorEtlSchedulerDO.class)
                        .eq(CollectorEtlSchedulerDO::getTaskId, task.getId()), false);
                if (schedule != null && schedule.getDsId() != null && schedule.getDsId() > 0) {
                    DsStatusRespDTO stopped = dsSchedules.offlineScheduler(task.getSpaceCode(), schedule.getDsId());
                    offline = offline && stopped != null && stopped.isOk() && !Boolean.FALSE.equals(stopped.getData());
                    if (stopped != null && stopped.isOk() && !Boolean.FALSE.equals(stopped.getData())) {
                        schedules.lambdaUpdate().eq(CollectorEtlSchedulerDO::getId, schedule.getId()).set(CollectorEtlSchedulerDO::getStatus, "0").update();
                    }
                }
            } catch (RuntimeException ex) { offline = false; }
            JSONObject meta = recovery(fresh); meta.put("offlineAttempted", true); meta.put("offlineConfirmed", offline);
            fresh.setAiRawResult(envelope(fresh, meta));
            if (offline) tasks.update(null, Wrappers.lambdaUpdate(CollectorEtlTaskDO.class).eq(CollectorEtlTaskDO::getId, task.getId()).set(CollectorEtlTaskDO::getStatus, "0"));
            fresh.setReason(fresh.getReason() + (offline ? "；任务及调度已确认下线" : "；远端下线未确认，请人工核对"));
            if (events.transition(fresh, fresh.getActionStatus()) != 1) throw new ServiceException("通知状态冲突");
                return null;
            });
            CollectorEtlTaskOpsEventDO afterOffline = events.selectById(selected.getId());
            if (afterOffline == null || "CANCELLED".equals(afterOffline.getActionStatus())) return;
        }
        try {
            new TransactionTemplate(transactionManager).execute(tx -> {
                runs.lockTask(task.getId());
                CollectorEtlTaskOpsEventDO event = events.lockEvent(selected.getId());
                if (event == null || !recovery(event).getBooleanValue("notificationPending")) return null;
                JSONObject meta = recovery(event);
                Set<Long> recipients = new TreeSet<>();
                if (policy.getNotifyUsers() != null) for (String value : policy.getNotifyUsers().split(",")) {
                    if (value.trim().matches("[0-9]+")) recipients.add(Long.valueOf(value.trim()));
                }
                for (Long receiver : recipients) {
                    if (!spaces.hasUserSpace(receiver, task.getSpaceId(), task.getSpaceCode())) continue;
                    com.datamaster.module.system.api.message.dto.SystemNoticeReqDTO notice = new com.datamaster.module.system.api.message.dto.SystemNoticeReqDTO();
                    notice.setReceiverId(receiver); notice.setEntityId(task.getId());
                    notice.setTitle("数据集成自动恢复：" + event.getActionStatus());
                    notice.setContent(ComponentRecoverySupport.sanitize(task.getName() + "；" + event.getReason()));
                    if (!Boolean.TRUE.equals(notices.sendSystemNotice(notice))) throw new ServiceException("站内通知保存失败");
                }
                meta.put("notificationPending", false); event.setAiRawResult(envelope(event, meta));
                if (events.transition(event, event.getActionStatus()) != 1) throw new ServiceException("通知标记保存失败");
                return null;
            });
        } catch (RuntimeException ex) {
            new TransactionTemplate(transactionManager).execute(tx -> {
                runs.lockTask(task.getId());
                CollectorEtlTaskOpsEventDO event = events.lockEvent(selected.getId());
                if (event == null) return null;
                JSONObject meta = recovery(event); int failures = meta.getIntValue("noticeFailures") + 1;
                meta.put("noticeFailures", failures); meta.put("noticeRetryAt", System.currentTimeMillis() + 60000L);
                if (failures >= 3) meta.put("notificationPending", false);
                event.setAiRawResult(envelope(event, meta)); events.transition(event, event.getActionStatus()); return null;
            });
        }
    }

    private boolean safeIncremental(Long taskId) {
        CollectorEtlTaskExtDO ext = extensions.getByTaskId(taskId);
        if (ext == null || ext.getFlinkxJobTemplateJson() == null) return false;
        JSONObject snapshot = JSON.parseObject(ext.getFlinkxJobTemplateJson());
        JSONObject stages = snapshot.getJSONObject("stages");
        if (!CollectorEtlStageRuntimeService.KIND.equals(snapshot.getString("kind")) || stages == null || stages.isEmpty()) return false;
        for (String key : stages.keySet()) {
            JSONObject config = stages.getJSONObject(key).getJSONObject("config");
            JSONObject output = config.getJSONObject("output");
            if (config.getJSONObject("incremental") == null || config.getJSONObject("incremental").getJSONArray("sources") == null
                    || config.getJSONObject("incremental").getJSONArray("sources").isEmpty() || output == null
                    || !"3".equals(output.getString("writeModeType")) || hasSql(output.get("preSql")) || hasSql(output.get("postSql"))) return false;
        }
        return true;
    }

    private static boolean hasSql(Object sql) { return sql != null && !sql.toString().trim().isEmpty() && !"[]".equals(sql.toString()); }
    private static int budget(CollectorEtlTaskOpsPolicyDO policy) { return Math.max(0, Math.min(10, policy.getMaxRecoverTimes() == null ? 1 : policy.getMaxRecoverTimes())); }
    public static boolean terminal(String state) { return Arrays.asList("SUCCESS", "EXHAUSTED", "MANUAL", "CANCELLED").contains(state); }
    public static JSONObject recovery(CollectorEtlTaskOpsEventDO event) {
        JSONObject data = event.getAiRawResult() == null ? new JSONObject() : JSON.parseObject(event.getAiRawResult());
        JSONObject meta = data.getJSONObject("recovery"); return meta == null ? new JSONObject() : meta;
    }
    private static String envelope(CollectorEtlTaskOpsEventDO event, JSONObject meta) {
        JSONObject data = event.getAiRawResult() == null ? new JSONObject() : JSON.parseObject(event.getAiRawResult());
        data.put("recovery", meta); return data.toJSONString();
    }
    private void transition(CollectorEtlTaskOpsEventDO event, String state, String reason) {
        String expected = event.getActionStatus(); event.setActionStatus(state); event.setReason(reason);
        if (Arrays.asList("SUCCESS", "EXHAUSTED", "MANUAL").contains(state) && !state.equals(expected)) {
            JSONObject meta = recovery(event); meta.put("notificationPending", true); event.setAiRawResult(envelope(event, meta));
        }
        if (events.transition(event, expected) != 1) throw new ServiceException("恢复事件状态冲突");
        dispatchAfterCommit(event.getId());
    }
}
