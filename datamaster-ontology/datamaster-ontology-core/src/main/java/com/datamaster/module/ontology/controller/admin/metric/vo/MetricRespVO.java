package com.datamaster.module.ontology.controller.admin.metric.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;
import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = true)
public class MetricRespVO extends MetricSaveReqVO {
    private String status;
    private Integer publishedVersion;
    private Boolean draftChanged;
    private Map<String, Object> sourceSnapshot;
}
