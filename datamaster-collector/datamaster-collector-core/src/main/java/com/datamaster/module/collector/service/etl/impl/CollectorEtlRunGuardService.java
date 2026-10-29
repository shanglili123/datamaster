package com.datamaster.module.collector.service.etl.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.datamaster.api.ds.api.service.etl.IDsEtlTaskService;
import com.datamaster.common.exception.ServiceException;
import com.datamaster.module.collector.dal.dataobject.etl.*;
import com.datamaster.module.collector.dal.mapper.etl.*;
import com.datamaster.redis.service.IRedisService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import java.util.*;

/** 运行占用以数据库为准；Redis 仅作为可续租的快速互斥层。 */
@Slf4j
@Service
public class CollectorEtlRunGuardService {
    @Resource private CollectorEtlRunMapper runMapper;
    @Resource private CollectorEtlTaskMapper taskMapper;
    @Resource private com.datamaster.module.collector.service.etl.ICollectorEtlTaskExtService extService;
    @Resource private IDsEtlTaskService dsTaskService;
    @Resource private com.datamaster.api.ds.api.service.etl.IDsEtlExecutorService dsExecutorService;
    @Resource private IRedisService redisService;
    @Resource @org.springframework.context.annotation.Lazy private com.datamaster.module.collector.service.etl.ICollectorEtlTaskInstanceService instanceService;
    @Resource private CollectorEtlStageRuntimeService stageRuntimeService;
    @Resource @org.springframework.context.annotation.Lazy private CollectorComponentRecoveryService recoveryService;
    @Resource private org.springframework.transaction.PlatformTransactionManager transactionManager;
    @Value("${ds.run_guard.enabled:false}") private boolean enabled;
    @Value("${ds.run_guard.lease_seconds:120}") private long leaseSeconds;

    /** 根HTTP按工作流code定位本次执行；返回本地执行ID给后续阶段。 */
    public CollectorEtlRunDO prepareLatestComponent(Long taskId, String resourcesJson) {
        if (!enabled) throw new ServiceException("运行保护未启用");
        return new TransactionTemplate(transactionManager).execute(status -> {
            if (runMapper.lockTask(taskId) == null) throw new ServiceException("任务不存在");
            CollectorEtlTaskDO task = taskMapper.selectById(taskId);
            CollectorEtlTaskExtDO ext = extService.getByTaskId(taskId);
            if (task == null || ext == null) throw new ServiceException("任务发布配置不存在");
            JSONObject latest = dsTaskService.getLatestWorkflowInstance(task.getSpaceCode(), ext.getEtlTaskCode());
            return prepareComponent(taskId, latest.getLong("id"), resourcesJson);
        });
    }

    public CollectorEtlRunDO prepareComponent(Long taskId, Long instanceId, String resourcesJson) {
        if (!enabled) throw new ServiceException("运行保护未启用");
        if (taskId == null || instanceId == null || instanceId <= 0) throw new ServiceException("无效的运行准入实例");
        return new TransactionTemplate(transactionManager).execute(status -> {
            if (runMapper.lockTask(taskId) == null) throw new ServiceException("运行所属任务不存在");
            CollectorEtlTaskDO task = taskMapper.selectById(taskId);
            if (task == null || !"1".equals(task.getStatus())) throw new ServiceException("组件任务未发布或已下线，禁止准入/恢复");
            CollectorEtlTaskExtDO ext = extService.getByTaskId(taskId);
            JSONObject snapshot = ext == null || ext.getFlinkxJobTemplateJson() == null ? null
                    : JSON.parseObject(ext.getFlinkxJobTemplateJson());
            if (snapshot == null || !CollectorEtlStageRuntimeService.KIND.equals(snapshot.getString("kind"))
                    || snapshot.getString("guardNodeCode") == null || snapshot.getString("resourcesJson") == null
                    || !snapshot.getString("resourcesJson").equals(resourcesJson)) {
                throw new ServiceException("准入请求与已发布组件快照不一致，请重新发布任务");
            }
            JSONObject remote = dsTaskService.getWorkflowInstance(task.getSpaceCode(), instanceId);
            if (!Objects.equals(snapshot.getString("workflowCode"), remote.getString("workflowDefinitionCode"))
                    || !Objects.equals(snapshot.getString("workflowVersion"), remote.getString("workflowDefinitionVersion"))
                    || !Arrays.asList("RUNNING_EXECUTION", "DELAY_EXECUTION").contains(remote.getString("state"))) {
                throw new ServiceException("运行实例不属于已发布组件版本或已结束");
            }
            List<com.datamaster.api.ds.api.etl.ds.TaskInstance> nodes = dsTaskService.listTaskInstancesStrict(task.getSpaceCode(), instanceId);
            boolean runningGuard = nodes.stream()
                    .anyMatch(node -> snapshot.getString("guardNodeCode").equals(node.getTaskCode())
                            && node.getState() != null && "RUNNING_EXECUTION".equals(node.getState().name()));
            if (!runningGuard) throw new ServiceException("工作流运行准入节点未在执行");
            instanceService.synchronizeComponentRun(taskId, remote, nodes);
            CollectorEtlRunDO run = prepare(taskId, instanceId, snapshot.getLong("workflowCode"), Collections.emptyList());
            recoveryService.admitted(run, snapshot.getInteger("workflowVersion"));
            return run;
        });
    }

    private CollectorEtlRunDO prepare(Long taskId, Long instanceId, Long workflowCode, Collection<String> targets) {
        if (!enabled) throw new ServiceException("运行保护未启用，请先完成运行表迁移和回调配置");
        if (taskId == null || instanceId == null || workflowCode == null || targets == null) {
            throw new ServiceException("运行保护缺少任务、实例或写目标集合");
        }
        if (leaseSeconds < 30) throw new ServiceException("运行租约至少30秒，请核对续租周期");
        SortedSet<String> keys = new TreeSet<>();
        keys.add("etl-task:" + taskId);
        CollectorEtlRunDO run = new TransactionTemplate(transactionManager).execute(status -> {
            if (runMapper.lockTask(taskId) == null) throw new ServiceException("运行所属任务不存在");
            CollectorEtlTaskDO task = taskMapper.selectById(taskId);
            if (task == null || !"1".equals(task.getStatus())) throw new ServiceException("组件任务未发布或已下线，禁止准入/恢复");
            CollectorEtlTaskExtDO ext = extService.getByTaskId(taskId);
            JSONObject instance = dsTaskService.getWorkflowInstance(task.getSpaceCode(), instanceId);
            if (ext == null || !String.valueOf(workflowCode).equals(ext.getEtlTaskCode())
                    || !String.valueOf(workflowCode).equals(instance.getString("workflowDefinitionCode"))
                    || !String.valueOf(ext.getEtlTaskVersion()).equals(instance.getString("workflowDefinitionVersion"))) {
                throw new ServiceException("准入过程中发布版本已变化，拒绝登记旧快照");
            }
            CollectorEtlRunDO candidate = new CollectorEtlRunDO();
            candidate.setTaskId(taskId); candidate.setWorkflowInstanceId(instanceId);
            candidate.setWorkflowCode(workflowCode); candidate.setOwnerToken(UUID.randomUUID().toString());
            boolean created = runMapper.insertIfAbsent(candidate) == 1;
            CollectorEtlRunDO current = runMapper.getByWorkflowInstance(taskId, instanceId);
            if (current == null) throw new ServiceException("流程实例运行owner初始化失败");
            current = runMapper.lockById(current.getId());
            if (!"ACTIVE".equals(current.getStatus())) throw new ServiceException("运行已暂停、待核对或结束，不能重新准入");
            if (!workflowCode.equals(current.getWorkflowCode())) {
                throw new ServiceException("运行写目标或工作流已变化，禁止重试覆盖原执行上下文");
            }
            if (created) {
                final String owner = current.getOwnerToken();
                org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
                        new org.springframework.transaction.support.TransactionSynchronization() {
                            @Override public void afterCompletion(int completion) {
                                if (completion != STATUS_ROLLED_BACK) return;
                                for (String key : keys) {
                                    try { redisService.deleteIfOwner(redisKey(key), owner); }
                                    catch (RuntimeException ex) { log.warn("准入回滚后的租约清理失败 taskId={}，等待TTL到期", taskId); }
                                }
                            }
                        });
            }
            if (runMapper.acquireTask(current) != 1) throw new ServiceException("同一任务已有运行资格，等待原实例确认结束");
            current.setResourcesJson(JSON.toJSONString(keys));
            // 首次准入在数据库事务提交前建立租约；对账器不会读到半初始化的运行。
            for (String key : keys) {
                String redisKey = redisKey(key);
                if (!redisService.renewIfOwner(redisKey, current.getOwnerToken(), leaseSeconds)
                        && !(created && redisService.setIfAbsent(redisKey, current.getOwnerToken(), leaseSeconds))) {
                    throw new ServiceException("运行租约冲突，禁止启动写入");
                }
            }
            runMapper.heartbeat(current.getId(), current.getOwnerToken());
            return current;
        });
        return run;
    }

    /** 暂停恢复保留原owner和窗口；失租或终态实例不能通过恢复按钮重新获得资格。 */
    public com.datamaster.api.ds.api.base.DsStatusRespDTO resumePaused(Long taskId, Long instanceId) {
        if (!enabled) throw new ServiceException("运行保护未启用");
        if (taskId == null || instanceId == null || instanceId <= 0) throw new ServiceException("缺少有效的暂停实例身份");
        return new TransactionTemplate(transactionManager).execute(status -> {
            if (runMapper.lockTask(taskId) == null) throw new ServiceException("运行所属任务不存在");
            CollectorEtlRunDO run = runMapper.getByWorkflowInstance(taskId, instanceId);
            if (run != null) run = runMapper.lockById(run.getId());
            if (run == null || !active(run)) throw new ServiceException("实例没有有效运行登记，不能恢复");
            CollectorEtlTaskDO task = taskMapper.selectById(taskId);
            if (task == null || !"1".equals(task.getStatus())) throw new ServiceException("组件任务未发布或已下线，禁止准入/恢复");
            CollectorEtlTaskExtDO ext = extService.getByTaskId(taskId);
            JSONObject snapshot = ext == null || ext.getFlinkxJobTemplateJson() == null ? null
                    : JSON.parseObject(ext.getFlinkxJobTemplateJson());
            JSONObject remote = dsTaskService.getWorkflowInstance(task.getSpaceCode(), instanceId);
            if (snapshot == null || !CollectorEtlStageRuntimeService.KIND.equals(snapshot.getString("kind"))
                    || !Objects.equals(snapshot.getString("workflowCode"), remote.getString("workflowDefinitionCode"))
                    || !Objects.equals(snapshot.getString("workflowVersion"), remote.getString("workflowDefinitionVersion"))) {
                throw new ServiceException("暂停实例与组件发布快照不一致，不能恢复");
            }
            if (!"PAUSE".equals(remote.getString("state"))
                    || !String.valueOf(run.getWorkflowCode()).equals(remote.getString("workflowDefinitionCode"))) {
                throw new ServiceException("仅已确认暂停的原工作流实例可以恢复");
            }
            if (!run.getId().equals(runMapper.taskOwner(taskId))
                    || !redisService.renewIfOwner(redisKey("etl-task:" + taskId), run.getOwnerToken(), leaseSeconds)) {
                throw new ServiceException("运行所有权或租约已失效，请确认停止后重跑整个任务");
            }
            com.datamaster.api.ds.api.base.DsStatusRespDTO response = dsExecutorService.execute(com.datamaster.api.ds.api.etl.DSExecuteDTO.builder()
                    .processInstanceId(instanceId).executeType(com.datamaster.common.enums.ExecuteType.RECOVER_SUSPENDED_PROCESS)
                    .build(), task.getSpaceCode());
            if (response != null && Boolean.TRUE.equals(response.getSuccess())) {
                mark(run, "ACTIVE");
                runMapper.mirrorTask(taskId, run.getId(), run.getOwnerToken(), "ACTIVE");
            }
            return response;
        });
    }

    @Scheduled(fixedDelayString = "${ds.run_guard.reconcile_delay_ms:15000}")
    public void reconcile() {
        if (!enabled) return;
        long afterId = 0L;
        while (true) {
            List<CollectorEtlRunDO> runs;
            try { runs = runMapper.findUnfinished(afterId); }
            catch (RuntimeException ex) { log.error("运行保护无法读取占用记录，请检查迁移和数据库连接", ex); return; }
            for (CollectorEtlRunDO run : runs) {
                afterId = run.getId();
                try { reconcile(run); }
                catch (RuntimeException ex) {
                    try { mark(run, "UNCERTAIN"); }
                    catch (RuntimeException markFailure) { log.warn("运行待核对状态更新失败 runId={}", run.getId()); }
                    log.warn("运行状态待核对，保留写目标占用 runId={} instanceId={}", run.getId(), run.getWorkflowInstanceId());
                }
            }
            if (runs.size() < 200) break;
        }
    }

    private void reconcile(CollectorEtlRunDO run) {
        new TransactionTemplate(transactionManager).execute(status -> {
            if (runMapper.lockTask(run.getTaskId()) == null) throw new ServiceException("运行所属任务已不存在，禁止自动释放");
            CollectorEtlRunDO current = runMapper.lockById(run.getId());
            if (active(current) && run.getOwnerToken().equals(current.getOwnerToken())) reconcileLocked(current);
            return null;
        });
    }

    private void reconcileLocked(CollectorEtlRunDO run) {
        CollectorEtlTaskDO task = taskMapper.selectById(run.getTaskId());
        if (task == null) throw new ServiceException("运行所属任务已不存在，禁止自动释放");
        if (!Objects.equals(run.getId(), runMapper.taskOwner(run.getTaskId()))) throw new ServiceException("持久运行owner不一致，禁止收尾");
        JSONObject remote = dsTaskService.getWorkflowInstance(task.getSpaceCode(), run.getWorkflowInstanceId());
        if (!String.valueOf(run.getWorkflowCode()).equals(String.valueOf(remote.get("workflowDefinitionCode")))) {
            throw new ServiceException("DS 实例与已登记工作流不一致");
        }
        List<com.datamaster.api.ds.api.etl.ds.TaskInstance> nodes = dsTaskService.listTaskInstancesStrict(task.getSpaceCode(), run.getWorkflowInstanceId());
        String state = remote.getString("state");
        String terminal = "SUCCESS".equals(state) ? "SUCCESS" : "FAILURE".equals(state) ? "FAILURE"
                : "STOP".equals(state) ? "STOP" : null;
        if (terminal != null) {
            // 流程终态且结束时间明确。子任务仍活动/查询结果不完整时不能放行下一owner。
            if (remote.get("endTime") == null || nodes.stream()
                    .anyMatch(instance -> instance.getState() == null || !terminalTaskState(instance.getState().name()))) {
                throw new ServiceException("流程终态但写任务尚未确认退出");
            }
            instanceService.synchronizeComponentRun(run.getTaskId(), remote, nodes);
            stageRuntimeService.finish(run.getTaskId(), run.getWorkflowInstanceId(), run.getWorkflowCode(), "SUCCESS".equals(terminal), nodes);
            if (runMapper.compareAndSetTerminal(run.getId(), run.getOwnerToken(), run.getStatus(), terminal) != 1) {
                throw new ServiceException("运行终态提交冲突，回滚本次水位和实例更新");
            }
            if (runMapper.releaseTask(run.getTaskId(), run.getId(), run.getOwnerToken()) != 1) throw new ServiceException("任务运行资格释放冲突");
            recoveryService.finished(run, terminal);
            org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
                    new org.springframework.transaction.support.TransactionSynchronization() {
                        @Override public void afterCommit() {
                            for (String key : Collections.singletonList("etl-task:" + run.getTaskId())) {
                                try { redisService.deleteIfOwner(redisKey(key), run.getOwnerToken()); }
                                catch (RuntimeException ex) { log.warn("运行已收尾，租约清理等待TTL runId={}", run.getId()); }
                            }
                        }
                    });
            return;
        }
        boolean leaseLost = false;
        for (String key : Collections.singletonList("etl-task:" + run.getTaskId())) {
            if (!redisService.renewIfOwner(redisKey(key), run.getOwnerToken(), leaseSeconds)) leaseLost = true;
        }
        if (leaseLost) {
            mark(run, "UNCERTAIN");
            dsTaskService.stopWorkflowInstance(task.getSpaceCode(), run.getWorkflowInstanceId());
            return;
        }
        instanceService.synchronizeComponentRun(run.getTaskId(), remote, nodes);
        String next = "PAUSE".equals(state) || "READY_PAUSE".equals(state) ? "PAUSED"
                : Arrays.asList("RUNNING_EXECUTION", "DELAY_EXECUTION").contains(state) ? "ACTIVE" : "UNCERTAIN";
        mark(run, next);
        runMapper.heartbeat(run.getId(), run.getOwnerToken());
        runMapper.mirrorTask(run.getTaskId(), run.getId(), run.getOwnerToken(), next);
    }

    private void mark(CollectorEtlRunDO run, String state) {
        if (active(run) && !state.equals(run.getStatus())) {
            if (runMapper.compareAndSetActiveStatus(run.getId(), run.getOwnerToken(), run.getStatus(), state) != 1) {
                throw new ServiceException("运行状态已变化，请重新核对");
            }
        }
    }
    private boolean active(CollectorEtlRunDO run) {
        return run != null && Arrays.asList("ACTIVE", "UNCERTAIN", "PAUSED").contains(run.getStatus());
    }
    private boolean terminalTaskState(String state) {
        return Arrays.asList("SUCCESS", "FAILURE", "STOP", "KILL", "FORCED_SUCCESS").contains(state);
    }
    private String redisKey(String resource) {
        return "etl:run-resource:" + com.datamaster.common.database.utils.MD5Util.encrypt(resource);
    }
}
