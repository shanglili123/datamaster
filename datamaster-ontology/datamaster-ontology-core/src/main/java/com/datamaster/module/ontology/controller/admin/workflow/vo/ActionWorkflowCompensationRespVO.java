package com.datamaster.module.ontology.controller.admin.workflow.vo;

import com.datamaster.module.ontology.controller.admin.action.vo.ExecutionRespVO;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/** Workflow 补偿计划提交结果。补偿动作只会进入人工审批/执行链，不会自动执行。 */
@Data
public class ActionWorkflowCompensationRespVO {
    private Long runId;
    private String compensationStatus;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date compensationRequestedTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date compensationFinishedTime;
    private String compensationError;
    private Integer retryableCount;
    private Integer eligibleCount;
    private List<ExecutionRespVO> executions = new ArrayList<>();
    private List<String> errors = new ArrayList<>();
    private String message;
}
