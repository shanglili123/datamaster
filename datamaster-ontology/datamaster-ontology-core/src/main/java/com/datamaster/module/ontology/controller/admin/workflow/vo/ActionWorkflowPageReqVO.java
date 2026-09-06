package com.datamaster.module.ontology.controller.admin.workflow.vo;

import com.datamaster.common.core.page.PageParam;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class ActionWorkflowPageReqVO extends PageParam {
    private static final long serialVersionUID = 1L;
    private Long ontologyId;
    private String name;
    private String status;
}
