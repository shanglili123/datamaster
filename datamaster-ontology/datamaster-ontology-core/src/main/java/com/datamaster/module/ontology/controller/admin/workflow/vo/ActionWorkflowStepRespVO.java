package com.datamaster.module.ontology.controller.admin.workflow.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

@Data
public class ActionWorkflowStepRespVO {
    private Long id;
    private Long runId;
    private String nodeKey;
    private String nodeType;
    private Long actionId;
    private Long executionId;
    private String status;
    private String inputParams;
    private String outputJson;
    private String errorMessage;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date startedTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date finishedTime;
    private Integer attemptNo;
}
