package com.datamaster.module.ontology.controller.admin.workflow.vo;

import lombok.Data;
import java.util.Date;
import java.util.List;

@Data
public class ActionWorkflowRespVO {
    private Long id;
    private Long ontologyId;
    private String code;
    private String name;
    private Long triggerConceptId;
    private Integer version;
    private String status;
    private String triggerRef;
    private String inputSchema;
    private String outputSchema;
    private String failurePolicy;
    private Boolean enabled;
    private String description;
    private Date createTime;
    private List<ActionWorkflowNodeVO> nodes;
    private List<ActionWorkflowEdgeVO> edges;
}
