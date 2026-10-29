package com.datamaster.module.ontology.controller.admin.analysis.vo;

import lombok.Data;
import java.util.Date;

@Data
public class MetricAnalysisRespVO {
    private Long id;
    private Long ontologyId;
    private String name;
    private String description;
    private MetricQueryReqVO query;
    private Integer revision;
    private Date createTime;
    private Date updateTime;
}
