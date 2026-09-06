package com.datamaster.module.ontology.controller.admin.workflow.vo;

import lombok.Data;

@Data
public class ActionWorkflowEdgeVO {
    private Long id;
    private String edgeKey;
    private String fromNodeKey;
    private String toNodeKey;
    private String conditionExpr;
    private Integer priority;
}
