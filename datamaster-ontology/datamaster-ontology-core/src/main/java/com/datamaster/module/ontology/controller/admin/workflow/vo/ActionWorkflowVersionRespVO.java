package com.datamaster.module.ontology.controller.admin.workflow.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

/** Workflow 发布版本摘要。 */
@Data
public class ActionWorkflowVersionRespVO {
    private Long id;
    private Long workflowId;
    private Integer version;
    private String status;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;
    private Long creatorId;
    private String createBy;
}
