package com.datamaster.module.ontology.controller.admin.workflow.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class ActionWorkflowRunRespVO {
    private Long id;
    private Long workflowId;
    private String idempotencyKey;
    private String status;
    private String inputParams;
    private String outputContext;
    private String currentNodeKey;
    private String errorMessage;
    private Integer cancelRequested;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date startedTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date finishedTime;
    private String compensationStatus;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date compensationRequestedTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date compensationFinishedTime;
    private String compensationError;
    private Integer compensationRetryableCount;
    private List<ActionWorkflowStepRespVO> steps;
}
