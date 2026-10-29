package com.datamaster.module.ontology.controller.admin.analysis.vo;

import lombok.Data;
import javax.validation.Valid;
import javax.validation.constraints.*;

@Data
public class MetricAnalysisSaveReqVO {
    private Long id;
    @NotNull private Long ontologyId;
    @NotBlank @Size(max = 100) private String name;
    @Size(max = 1000) private String description;
    @NotNull @Valid private MetricQueryReqVO query;
    private Integer revision;
}
