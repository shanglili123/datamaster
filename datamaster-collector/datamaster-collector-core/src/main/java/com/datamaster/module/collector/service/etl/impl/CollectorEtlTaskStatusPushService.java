package com.datamaster.module.collector.service.etl.impl;

import com.datamaster.common.enums.Flag;
import com.datamaster.module.collector.controller.admin.etl.websocket.CollectorEtlTaskStatusMessage;
import com.datamaster.module.collector.controller.admin.etl.websocket.CollectorEtlTaskStatusWebSocketServer;
import com.datamaster.module.collector.dal.dataobject.etl.CollectorEtlTaskInstanceDO;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.Date;

@Service
public class CollectorEtlTaskStatusPushService {

    private static final String MESSAGE_TYPE = "etl-task-status";
    private static final String DATETIME_PATTERN = "yyyy-MM-dd HH:mm:ss";

    @javax.annotation.Resource
    private com.datamaster.module.collector.dal.mapper.etl.CollectorEtlTaskInstanceMapper instanceMapper;
    @javax.annotation.Resource
    private com.datamaster.module.collector.service.etl.ICollectorEtlTaskExtService extService;

    @org.springframework.transaction.annotation.Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public void pushComponentTaskInstanceStatus(Long taskId, Long expectedDsId) {
        if (instanceMapper.lockOwnerTask(taskId) == null) return;
        CollectorEtlTaskInstanceDO latest = instanceMapper.latestMainInstance(taskId);
        if (latest != null && java.util.Objects.equals(expectedDsId, latest.getDsId())) {
            broadcast(latest, String.valueOf(latest.getDsId()));
        }
    }

    public void pushTaskInstanceStatus(CollectorEtlTaskInstanceDO instance) {
        if (instance != null && instance.getTaskId() != null) {
            com.datamaster.module.collector.dal.dataobject.etl.CollectorEtlTaskExtDO ext = extService.getByTaskId(instance.getTaskId());
            if (ext != null && ext.getFlinkxJobTemplateJson() != null
                    && CollectorEtlStageRuntimeService.KIND.equals(com.alibaba.fastjson2.JSON.parseObject(ext.getFlinkxJobTemplateJson()).getString("kind"))) {
                // 组件运行只由提交后的权威路径推送，不能经旧回调退回匿名广播。
                return;
            }
        }
        broadcast(instance, null);
    }

    private void broadcast(CollectorEtlTaskInstanceDO instance, String dsId) {
        if (instance == null || instance.getTaskId() == null || isSubTask(instance)) {
            return;
        }
        CollectorEtlTaskStatusMessage message = CollectorEtlTaskStatusMessage.builder()
                .type(MESSAGE_TYPE)
                .taskId(String.valueOf(instance.getTaskId()))
                .taskInstanceId(instance.getId())
                .spaceId(instance.getSpaceId())
                .spaceCode(instance.getSpaceCode())
                .taskType(instance.getTaskType())
                .lastExecuteStatus(instance.getStatus())
                .lastExecuteDsId(dsId)
                .lastExecuteTime(formatLastExecuteTime(instance))
                .build();
        if (dsId == null) CollectorEtlTaskStatusWebSocketServer.broadcast(message);
        else CollectorEtlTaskStatusWebSocketServer.broadcastComponent(message);
    }

    private boolean isSubTask(CollectorEtlTaskInstanceDO instance) {
        return instance.getParentTaskInstanceId() != null
                || String.valueOf(Flag.YES.getCode()).equals(instance.getSubTaskFlag());
    }

    private String formatLastExecuteTime(CollectorEtlTaskInstanceDO instance) {
        Date lastExecuteTime = instance.getEndTime() != null
                ? instance.getEndTime()
                : (instance.getStartTime() != null ? instance.getStartTime() : instance.getCreateTime());
        return lastExecuteTime == null ? null : new SimpleDateFormat(DATETIME_PATTERN).format(lastExecuteTime);
    }
}
