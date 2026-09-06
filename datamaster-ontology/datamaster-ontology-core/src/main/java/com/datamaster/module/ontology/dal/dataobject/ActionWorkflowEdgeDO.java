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
@TableName("ONT_ACTION_WORKFLOW_EDGE")
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ActionWorkflowEdgeDO extends BaseEntity {
    private Long workflowId;
    private String edgeKey;
    private String fromNodeKey;
    private String toNodeKey;
    private String conditionExpr;
    private Integer priority;
    @TableLogic
    private Integer delFlag;
}
