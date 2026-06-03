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

    public void pushTaskInstanceStatus(CollectorEtlTaskInstanceDO instance) {
        if (instance == null || instance.getTaskId() == null || isSubTask(instance)) {
            return;
        }
        CollectorEtlTaskStatusWebSocketServer.broadcast(CollectorEtlTaskStatusMessage.builder()
                .type(MESSAGE_TYPE)
                .taskId(instance.getTaskId())
                .taskInstanceId(instance.getId())
                .projectId(instance.getProjectId())
                .projectCode(instance.getProjectCode())
                .taskType(instance.getTaskType())
                .lastExecuteStatus(instance.getStatus())
                .lastExecuteTime(formatLastExecuteTime(instance))
                .build());
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
