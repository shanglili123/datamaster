package com.datamaster.module.collector.service.etl.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONWriter;
import com.alibaba.fastjson2.TypeReference;
import com.datamaster.common.exception.ServiceException;
import com.datamaster.common.utils.uuid.IdUtils;
import com.datamaster.module.collector.dal.dataobject.etl.*;
import com.datamaster.module.collector.dal.mapper.etl.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import java.util.*;
import java.util.function.Predicate;
import java.util.function.Supplier;

/** 窗口、重放状态和成功水位保存在同一条阶段实例记录，所有写入先锁任务行。 */
@Service
public class CollectorEtlIncrementalWindowService {
    @Resource private CollectorEtlStageRecordMapper stageMapper;
    @Resource private CollectorEtlTaskInstanceMapper instanceMapper;
    @Resource private CollectorEtlNodeInstanceMapper nodeMapper;
    @Resource private com.datamaster.module.collector.service.etl.ICollectorEtlTaskExtService extService;
    @Resource private CollectorEtlRunMapper runMapper;
    @Resource private org.springframework.transaction.PlatformTransactionManager transactionManager;

    public CollectorEtlIncrementalWindowDO prepare(Long taskId, String stage, Long instanceId, String configHash,
                                                   Map<String, String> initialValues, Map<String, String> cursorTypes,
                                                   Supplier<Map<String, String>> sourceMax,
                                                   Predicate<Long> previousConfirmedStopped) {
        if (taskId == null || stage == null || stage.trim().isEmpty() || instanceId == null || instanceId <= 0
                || configHash == null || configHash.trim().isEmpty() || initialValues == null || initialValues.isEmpty()
                || cursorTypes == null || !initialValues.keySet().equals(cursorTypes.keySet())) {
            throw new ServiceException("阶段窗口参数不完整");
        }
        return new TransactionTemplate(transactionManager).execute(tx -> {
            requireOwner(taskId, instanceId);
            CollectorEtlTaskInstanceDO parent = instanceMapper.selectById(instanceId);
            if (parent == null || !taskId.equals(parent.getTaskId())) throw new ServiceException("阶段所属流程未登记");
            CollectorEtlNodeInstanceDO existing = stageMapper.current(taskId, stage, instanceId);
            if (existing != null && existing.getIncrementalWindowStatus() != null) {
                requireHash(existing, configHash);
                if (!CollectorEtlIncrementalWindowDO.PREPARED.equals(existing.getIncrementalWindowStatus())) {
                    throw new ServiceException("本实例窗口已结束");
                }
                return asWindow(taskId, instanceId, existing);
            }

            CollectorEtlNodeInstanceDO previous = stageMapper.latest(taskId, stage);
            Map<String, String> start = new TreeMap<>(initialValues);
            Map<String, String> end = null;
            Long replayFrom = null;
            if (previous != null) {
                requireHash(previous, configHash);
                if (!Arrays.asList(CollectorEtlIncrementalWindowDO.SUCCESS, CollectorEtlIncrementalWindowDO.FAILURE,
                        CollectorEtlIncrementalWindowDO.PREPARED).contains(previous.getIncrementalWindowStatus())) {
                    throw new ServiceException("历史阶段窗口状态无效，禁止自动重算");
                }
                if (CollectorEtlIncrementalWindowDO.SUCCESS.equals(previous.getIncrementalWindowStatus())) {
                    start = parse(previous.getIncrementalSuccessfulValue());
                } else {
                    if (!previousConfirmedStopped.test(previous.getTaskInstanceId())) {
                        throw new ServiceException("旧实例未确认结束，禁止接管");
                    }
                    start = parse(previous.getIncrementalStartValue());
                    end = parse(previous.getIncrementalEndValue());
                    replayFrom = previous.getId();
                }
            }
            if (!start.keySet().equals(initialValues.keySet())) throw new ServiceException("成功水位来源集合不一致");
            if (end == null) {
                end = new TreeMap<>(sourceMax.get());
                if (!end.keySet().equals(start.keySet())) throw new ServiceException("源边界集合不完整");
                for (Map.Entry<String, String> boundary : end.entrySet()) {
                    String low = start.get(boundary.getKey());
                    if (low != null && boundary.getValue() != null
                            && com.datamaster.module.collector.utils.StageIncrementalSql.compare(
                                    boundary.getValue(), low, cursorTypes.get(boundary.getKey())) < 0) {
                        boundary.setValue(null);
                    }
                }
            }
            if (!end.keySet().equals(start.keySet())) throw new ServiceException("重放窗口来源集合不完整");
            CollectorEtlTaskExtDO ext = extService.getByTaskId(taskId);
            if (ext == null || ext.getFlinkxJobTemplateJson() == null) throw new ServiceException("阶段发布快照缺失");
            com.alibaba.fastjson2.JSONObject stages = JSON.parseObject(ext.getFlinkxJobTemplateJson()).getJSONObject("stages");
            com.alibaba.fastjson2.JSONObject published = stages == null ? null : stages.getJSONObject(stage);
            if (published == null || published.getString("dsNodeCode") == null) throw new ServiceException("发布快照未登记该阶段数据节点");
            String dataNodeCode = published.getString("dsNodeCode");
            if (existing == null) existing = nodeMapper.findByParentCode(parent.getId(), dataNodeCode);
            if (existing != null && existing.getStableStageKey() != null && !stage.equals(existing.getStableStageKey())) {
                throw new ServiceException("数据节点已关联其他阶段，禁止覆盖窗口");
            }
            CollectorEtlNodeInstanceDO row = existing;
            if (row == null) {
                row = CollectorEtlNodeInstanceDO.builder().id(IdUtils.generateArtificialId())
                        .taskInstanceId(parent.getId()).taskInstanceName(parent.getName()).taskType(parent.getTaskType())
                        .spaceId(parent.getSpaceId()).spaceCode(parent.getSpaceCode()).dsTaskInstanceId(parent.getDsId())
                        .stableStageKey(stage).name(stage).nodeType("CHUNJUN").nodeCode(dataNodeCode).nodeVersion(0)
                        .nodeId(0L).parameters("{}").status("0").build();
            }
            row.setStableStageKey(stage);
            row.setIncrementalConfigHash(configHash);
            row.setIncrementalStartValue(json(start));
            row.setIncrementalEndValue(json(end));
            row.setIncrementalWindowEmpty(allNull(end));
            row.setIncrementalWindowStatus(CollectorEtlIncrementalWindowDO.PREPARED);
            row.setIncrementalReplayFromId(replayFrom);
            int changed = existing == null ? stageMapper.insertWindow(row) : stageMapper.attachWindow(row);
            if (changed != 1) throw new ServiceException("保存阶段窗口失败");
            return asWindow(taskId, instanceId, row);
        });
    }

    public void finish(Long taskId, String stage, Long instanceId, String hash, boolean success) {
        new TransactionTemplate(transactionManager).execute(tx -> {
            requireOwner(taskId, instanceId);
            CollectorEtlNodeInstanceDO row = stageMapper.current(taskId, stage, instanceId);
            if (row == null || row.getIncrementalWindowStatus() == null) return null;
            requireHash(row, hash);
            if (!CollectorEtlIncrementalWindowDO.PREPARED.equals(row.getIncrementalWindowStatus())) return null;
            Map<String, String> committed = parse(row.getIncrementalStartValue());
            Map<String, String> end = parse(row.getIncrementalEndValue());
            if (!committed.keySet().equals(end.keySet())) throw new ServiceException("固定窗口来源不完整");
            if (success) {
                for (Map.Entry<String, String> value : end.entrySet()) {
                    if (value.getValue() != null) committed.put(value.getKey(), value.getValue());
                }
            }
            if (stageMapper.finish(row.getId(), success ? CollectorEtlIncrementalWindowDO.SUCCESS
                    : CollectorEtlIncrementalWindowDO.FAILURE, success ? json(committed) : null) != 1) {
                throw new ServiceException("窗口终态冲突");
            }
            return null;
        });
    }

    @Transactional(propagation = Propagation.MANDATORY, rollbackFor = Exception.class)
    public void describeWindow(Long task, String stage, Long instance, String code, Integer version, String sources) {
        requireOwner(task, instance);
        CollectorEtlNodeInstanceDO row = stageMapper.current(task, stage, instance);
        if (row == null || stageMapper.describe(row.getId(), code, version, sources) != 1) {
            throw new ServiceException("阶段来源快照保存失败");
        }
    }

    public CollectorEtlIncrementalWindowDO getWindow(Long task, String stage, Long instance) {
        CollectorEtlNodeInstanceDO row = stageMapper.current(task, stage, instance);
        return row == null || row.getIncrementalWindowStatus() == null ? null : asWindow(task, instance, row);
    }

    public List<CollectorEtlIncrementalWindowDO> listWindows(Long task, Long instance) {
        List<CollectorEtlIncrementalWindowDO> result = new ArrayList<>();
        for (CollectorEtlNodeInstanceDO row : stageMapper.list(task, instance)) result.add(asWindow(task, instance, row));
        return result;
    }

    private void requireOwner(Long task, Long instance) {
        if (stageMapper.lockTask(task) == null) throw new ServiceException("任务不存在");
        CollectorEtlRunDO run = runMapper.lockById(instance);
        if (run == null || !task.equals(run.getTaskId()) || !Objects.equals(run.getId(), runMapper.taskOwner(task))
                || !Arrays.asList("ACTIVE", "PAUSED", "UNCERTAIN").contains(run.getStatus())) {
            throw new ServiceException("运行owner已变化");
        }
    }

    private static void requireHash(CollectorEtlNodeInstanceDO row, String hash) {
        if (!Objects.equals(hash, row.getIncrementalConfigHash())) throw new ServiceException("阶段执行配置已变化，需要显式迁移水位");
    }

    private static CollectorEtlIncrementalWindowDO asWindow(Long task, Long instance, CollectorEtlNodeInstanceDO row) {
        CollectorEtlIncrementalWindowDO value = new CollectorEtlIncrementalWindowDO();
        value.setTaskId(task); value.setStageKey(row.getStableStageKey()); value.setInstanceId(instance);
        value.setStartValue(row.getIncrementalStartValue()); value.setEndValue(row.getIncrementalEndValue());
        value.setEmpty(row.getIncrementalWindowEmpty()); value.setStatus(row.getIncrementalWindowStatus());
        value.setSourcesJson(row.getIncrementalSourcesJson()); value.setReplayFromId(row.getIncrementalReplayFromId());
        return value;
    }

    public static Map<String, String> parse(String value) {
        return value == null || value.trim().isEmpty() ? new TreeMap<>()
                : JSON.parseObject(value, new TypeReference<TreeMap<String, String>>() {});
    }

    private static String json(Map<String, String> values) {
        return JSON.toJSONString(new TreeMap<>(values), JSONWriter.Feature.WriteNulls);
    }

    private static boolean allNull(Map<String, String> values) {
        for (String value : values.values()) if (value != null) return false;
        return true;
    }
}
