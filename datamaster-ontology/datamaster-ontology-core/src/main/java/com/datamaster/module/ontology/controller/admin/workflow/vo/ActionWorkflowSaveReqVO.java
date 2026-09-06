package com.datamaster.module.ontology.controller.admin.workflow.vo;

import lombok.Data;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.List;

@Data
public class ActionWorkflowSaveReqVO {
    private Long id;
    @NotNull(message = "本体ID不能为空")
    private Long ontologyId;
    private String code;
    @NotBlank(message = "编排名称不能为空")
    private String name;
    private Long triggerConceptId;
    private String triggerRef;
    private String inputSchema;
    private String outputSchema;
    private String failurePolicy;
    private String description;
    private List<ActionWorkflowNodeVO> nodes;
    private List<ActionWorkflowEdgeVO> edges;
}
