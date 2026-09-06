package com.datamaster.module.ontology.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.datamaster.module.ontology.dal.dataobject.ActionExecutionDO;
import com.datamaster.module.ontology.dal.mapper.ActionExecutionMapper;
import com.datamaster.module.ontology.service.IActionExecutionService;
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
 * 以 ONT_ACTION_EXECUTION 为持久队列的动作执行 Worker。
 */
@Slf4j
@Component
public class ActionExecutionWorker {

    private static final String STATUS_RUNNING = "RUNNING";
    private static final String STATUS_RECONCILIATION_REQUIRED = "RECONCILIATION_REQUIRED";

    @Resource
    private ActionExecutionMapper executionMapper;

    @Resource
    private IActionExecutionService executionService;

    @Resource(name = "threadPoolTaskExecutor")
    private ThreadPoolTaskExecutor taskExecutor;

    @Value("${datamaster.ontology.action-execution.worker-enabled:true}")
    private boolean workerEnabled;

    @Value("${datamaster.ontology.action-execution.batch-size:20}")
    private int batchSize;

    @Value("${datamaster.ontology.action-execution.lock-timeout-ms:600000}")
    private long lockTimeoutMs;

    /** 防止同一实例在任务尚未开始时被下一轮扫描重复塞入线程池。 */
    private final Set<Long> submitted = ConcurrentHashMap.newKeySet();

    @Scheduled(fixedDelayString = "${datamaster.ontology.action-execution.worker-delay-ms:1000}")
    public void scan() {
        if (!workerEnabled) {
            return;
        }
        markStaleExecutions();
        List<ActionExecutionDO> candidates = executionMapper.selectAutoExecuteCandidates(new Date(), batchSize);
        for (ActionExecutionDO candidate : candidates) {
            Long executionId = candidate.getId();
            if (!submitted.add(executionId)) {
                continue;
            }
            try {
                taskExecutor.submit(() -> {
                    try {
                        execute(executionId);
                    } finally {
                        submitted.remove(executionId);
                    }
                });
            } catch (Exception e) {
                submitted.remove(executionId);
                log.warn("自动动作提交线程池失败 executionId={}", executionId, e);
            }
        }
    }

    private void execute(Long executionId) {
        try {
            executionService.executeExecution(executionId);
        } catch (Exception e) {
            // 多实例/同批次重复扫描导致的 CAS 失败是正常竞争；真正执行失败已由执行服务落库。
            log.debug("自动动作执行未取得执行权或执行入口异常 executionId={}, message={}",
                    executionId, e.getMessage());
        }
    }

    private void markStaleExecutions() {
        Date lockedBefore = new Date(System.currentTimeMillis() - Math.max(lockTimeoutMs, 1000L));
        List<ActionExecutionDO> stale = executionMapper.selectStaleRunning(lockedBefore, batchSize);
        for (ActionExecutionDO execution : stale) {
            int updated = executionMapper.update(null, new LambdaUpdateWrapper<ActionExecutionDO>()
                    .eq(ActionExecutionDO::getId, execution.getId())
                    .eq(ActionExecutionDO::getStatus, STATUS_RUNNING)
                    .le(ActionExecutionDO::getLockTime, lockedBefore)
                    .set(ActionExecutionDO::getStatus, STATUS_RECONCILIATION_REQUIRED)
                    .set(ActionExecutionDO::getErrorCode, "STALE_EXECUTION_LOCK")
                    .set(ActionExecutionDO::getErrorMessage,
                            "执行锁超时，目标系统可能已完成写入，已停止自动重放，请人工对账")
                    .set(ActionExecutionDO::getLockTime, null)
                    .set(ActionExecutionDO::getLockOwner, null));
            if (updated > 0) {
                log.warn("动作执行锁超时，已标记需对账 executionId={}, previousLockOwner={}",
                        execution.getId(), execution.getLockOwner());
            }
        }
    }
}
