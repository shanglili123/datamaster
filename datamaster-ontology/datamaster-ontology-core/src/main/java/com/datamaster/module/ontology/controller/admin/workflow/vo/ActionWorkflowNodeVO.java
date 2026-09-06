package com.datamaster.module.ontology.controller.admin.workflow.vo;

import lombok.Data;

@Data
public class ActionWorkflowNodeVO {
    private Long id;
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
}
