package com.datamaster.module.ontology.dal.dataobject;

import com.baomidou.mybatisplus.annotation.TableName;
import com.datamaster.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/** Workflow 一次运行实例。 */
@Data
@TableName("ONT_ACTION_WORKFLOW_RUN")
@EqualsAndHashCode(callSuper = true)
public class ActionWorkflowRunDO extends BaseEntity {
    private Long workflowId;
    private String idempotencyKey;
    private String status;
    private String inputParams;
    private String outputContext;
    private String currentNodeKey;
    private String errorMessage;
    private Date startedTime;
    private Date finishedTime;
    private Integer cancelRequested;
    /** 补偿计划状态：NONE/REQUESTED/PARTIAL/COMPLETED/FAILED。 */
    private String compensationStatus;
    private Date compensationRequestedTime;
    private Date compensationFinishedTime;
    private String compensationError;
}
