package com.datamaster.module.ontology.controller.admin.analysis.vo;

import com.datamaster.common.core.page.PageParam;
import lombok.Data;
import lombok.EqualsAndHashCode;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Data
@EqualsAndHashCode(callSuper = true)
public class MetricAnalysisPageReqVO extends PageParam {
    @NotNull private Long ontologyId;
    @Size(max = 100) private String name;
    public MetricAnalysisPageReqVO() { setPageSize(20); }
}
