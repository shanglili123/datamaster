package com.datamaster.module.ontology.controller.admin.metric.vo;

import com.datamaster.common.core.page.PageParam;
import lombok.Data;
import lombok.EqualsAndHashCode;
import javax.validation.constraints.NotNull;

@Data
@EqualsAndHashCode(callSuper = true)
public class MetricPageReqVO extends PageParam {
    public MetricPageReqVO() { setPageSize(20); }
    @NotNull private Long ontologyId;
    private Long conceptId;
    private String name;
    private String status;
}
