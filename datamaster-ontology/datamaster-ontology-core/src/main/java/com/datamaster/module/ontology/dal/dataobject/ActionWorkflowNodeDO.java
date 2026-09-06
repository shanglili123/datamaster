package com.datamaster.module.ontology.dal.dataobject;

import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.datamaster.common.core.domain.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@TableName("ONT_ACTION_WORKFLOW_NODE")
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ActionWorkflowNodeDO extends BaseEntity {
    private Long workflowId;
    private String nodeKey;
    private String name;
    private String nodeType;
    private Long actionId;
    private String configJson;
    private Integer timeoutMs;
    private String retryPolicy;
    private Long compensationActionId;
    private Integer positionX;
    private Integer positionY;
    @TableLogic
    private Integer delFlag;
}
