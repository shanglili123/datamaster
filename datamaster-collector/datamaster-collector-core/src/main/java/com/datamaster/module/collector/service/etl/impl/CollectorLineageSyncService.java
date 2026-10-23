package com.datamaster.module.collector.service.etl.impl;

import com.alibaba.fastjson2.JSON;
import com.datamaster.metadata.api.lineage.dto.*;
import com.datamaster.metadata.api.service.lineage.LineageApiService;
import com.datamaster.module.collector.dal.dataobject.etl.CollectorEtlTaskDO;
import com.datamaster.module.collector.dal.dataobject.etl.CollectorLineageOutboxDO;
import com.datamaster.common.exception.ServiceException;
import com.datamaster.module.collector.dal.mapper.etl.CollectorLineageOutboxMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import javax.annotation.Resource;
import java.util.*;

/** Local durable delivery, independent from legacy Neo4j and external scheduler transactions. */
@Slf4j
@Service
public class CollectorLineageSyncService {
    @Resource private CollectorLineageOutboxMapper outbox;
    @Resource private com.datamaster.module.collector.dal.mapper.etl.CollectorEtlTaskMapper tasks;
    @Resource private com.datamaster.common.api.space.ISpaceApi spaces;
    @Resource private LineageApiService lineageApi;
    @Value("${datamaster.lineage.jdbc-enabled:false}") private boolean enabled;

    @Transactional(propagation = Propagation.REQUIRED)
    public void enqueue(LineageSnapshotDTO snapshot) {
        if (!enabled) return;
        // Serialize operations for one task, including lifecycle tombstones and replays.
        if (outbox.lockTask(snapshot.getTaskId()) == null) throw new ServiceException("血缘任务不存在");
        Long sequence = outbox.nextSequence();
        snapshot.setSequence(sequence);
        if (snapshot.getVersion() == null) snapshot.setVersion(String.valueOf(sequence));
        CollectorLineageOutboxDO event = new CollectorLineageOutboxDO();
        event.setId(sequence);
        event.setTaskId(snapshot.getTaskId());
        event.setSequence(sequence);
        event.setEventType(snapshot.getEventType());
        event.setVersion(snapshot.getVersion());
        event.setPayload(JSON.toJSONString(snapshot));
        event.setStatus("PENDING");
        event.setAttempts(0);
        event.setCreateTime(new Date());
        event.setNextAttemptAt(new Date());
        outbox.insert(event);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCommit() {
                // Scheduled polling consumes only committed rows; no writes in a completed transaction.
                log.debug("血缘同步事件已提交 taskId={}, sequence={}", snapshot.getTaskId(), sequence);
            }
        });
    }

    public boolean isEnabled() { return enabled; }

    @Transactional(propagation = Propagation.REQUIRED)
    public void lifecycle(CollectorEtlTaskDO task, String eventType) {
        if (!enabled || task == null) return;
        // ONLINE restores only the last captured published payload, never the current editable draft.
        if ("ACTIVE".equals(eventType)) {
            CollectorLineageOutboxDO published = outbox.latestSnapshot(task.getId());
            if (published != null) {
                LineageSnapshotDTO saved = JSON.parseObject(published.getPayload(), LineageSnapshotDTO.class);
                saved.setEventType("UPSERT");
                enqueue(saved);
                return;
            }
        }
        LineageSnapshotDTO snapshot = new LineageSnapshotDTO();
        snapshot.setTaskId(task.getId());
        snapshot.setTaskName(task.getName());
        snapshot.setTaskCode(task.getCode());
        snapshot.setTaskType(task.getType());
        snapshot.setSpaceId(task.getSpaceId());
        snapshot.setSpaceCode(task.getSpaceCode());
        snapshot.setEventType(eventType);
        snapshot.setVersion(task.getVersion() == null ? null : String.valueOf(task.getVersion()));
        snapshot.setCaptureStatus("ACTIVE".equals(eventType) ? "FAILED" : "COMPLETE");
        if ("ACTIVE".equals(eventType)) {
            snapshot.setEventType("UPSERT");
            snapshot.setErrorCode("NO_PUBLISHED_SNAPSHOT");
            snapshot.setUnits(Collections.emptyList());
        }
        enqueue(snapshot);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void replay(Long taskId, Long expectedSequence) {
        if (!enabled) throw new ServiceException("JDBC血缘未启用");
        if (outbox.lockTask(taskId) == null) throw new ServiceException("任务不存在");
        CollectorEtlTaskDO task = tasks.selectById(taskId);
        if (task == null || !spaces.hasUserSpace(com.datamaster.common.utils.SecurityUtils.getUserId(), task.getSpaceId(), task.getSpaceCode())) {
            throw new ServiceException("任务空间无访问权限");
        }
        CollectorLineageOutboxDO captured = outbox.latestSnapshot(taskId);
        if (captured == null || !Objects.equals(expectedSequence, captured.getSequence())) {
            throw new ServiceException("发布快照已变化，请重新预检");
        }
        LineageSnapshotDTO saved = JSON.parseObject(captured.getPayload(), LineageSnapshotDTO.class);
        if (!"1".equals(task.getStatus()) && !"-3".equals(task.getStatus())) {
            saved.setEventType("INACTIVE");
        }
        enqueue(saved);
    }

    @Scheduled(fixedDelayString = "${datamaster.lineage.sync-delay-ms:5000}")
    public void deliver() {
        if (!enabled) return;
        try {
            for (CollectorLineageOutboxDO event : outbox.due()) {
                String token = UUID.randomUUID().toString();
                if (outbox.claim(event.getId(), token) != 1) continue;
                int attempts = event.getAttempts() == null ? 1 : event.getAttempts() + 1;
                String status = "RETRY";
                String code = "SYNC_UNAVAILABLE";
                try {
                    LineageSyncResultDTO result = lineageApi.sync(JSON.parseObject(event.getPayload(), LineageSnapshotDTO.class));
                    if (result != null && Arrays.asList("APPLIED", "IGNORED").contains(result.getStatus())) {
                        status = "DONE";
                        code = null;
                    } else if (result != null) {
                        code = "DISABLED".equals(result.getStatus()) ? "SYNC_DISABLED" : "SYNC_REJECTED";
                    }
                } catch (Exception failure) {
                    log.warn("血缘同步重试 taskId={}, sequence={}", event.getTaskId(), event.getSequence());
                }
                if (!"DONE".equals(status) && attempts >= 12) status = "FAILED";
                long delay = Math.min(300000L, 5000L * (1L << Math.min(attempts, 6)));
                outbox.finish(event.getId(), token, status, attempts, code, new Date(System.currentTimeMillis() + delay));
            }
        } catch (Exception failure) {
            // A missing migration must never be mistaken for successful delivery.
            log.warn("血缘同步队列不可用，请核对 PostgreSQL 迁移及开关配置");
        }
    }
}
