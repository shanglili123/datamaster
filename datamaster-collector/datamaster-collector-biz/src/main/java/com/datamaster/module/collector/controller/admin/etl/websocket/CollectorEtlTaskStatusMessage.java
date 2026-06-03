package com.datamaster.module.collector.controller.admin.etl.websocket;

import lombok.Builder;
import lombok.Data;

/**
 * ETL task status websocket payload.
 */
@Data
@Builder
public class CollectorEtlTaskStatusMessage {

    private String type;

    private Long taskId;

    private Long taskInstanceId;

    private Long projectId;

    private String projectCode;

    private String taskType;

    private String lastExecuteStatus;

    private String lastExecuteTime;
}
