package com.datamaster.module.ontology.dal.dataobject;

import com.baomidou.mybatisplus.annotation.TableName;
import com.datamaster.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/** Workflow 节点运行记录。 */
@Data
@TableName("ONT_ACTION_WORKFLOW_STEP")
@EqualsAndHashCode(callSuper = true)
public class ActionWorkflowStepDO extends BaseEntity {
    private Long runId;
    private String nodeKey;
    private String nodeType;
    private Long actionId;
    private Long executionId;
    private String status;
    private String inputParams;
    private String outputJson;
    private String errorMessage;
    private Date startedTime;
    private Date finishedTime;
    private Integer attemptNo;
}
