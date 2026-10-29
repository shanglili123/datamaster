package com.datamaster.module.collector.controller.admin.etl.vo;

import lombok.Data;

/** A persisted stage/source boundary; never exposes connection settings or runtime job JSON. */
@Data
public class CollectorEtlStageWindowRespVO {
    private Long taskId;
    private Long taskInstanceId;
    private Long workflowInstanceId;
    private String stageKey;
    private String sourceNodeId;
    private String sourceTable;
    private String cursorColumn;
    private String cursorType;
    private Long replayFromId;
    private String startValue;
    private String endValue;
    private Boolean empty;
    private String status;
}
