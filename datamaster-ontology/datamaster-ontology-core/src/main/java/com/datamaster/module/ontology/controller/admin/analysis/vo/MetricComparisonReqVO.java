package com.datamaster.module.ontology.controller.admin.analysis.vo;

import lombok.Data;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

@Data
public class MetricComparisonReqVO {
    @Valid @NotNull private MetricQueryReqVO query;
    /** PREVIOUS_PERIOD (equal duration), PREVIOUS_MONTH, PREVIOUS_YEAR, or CUSTOM. */
    private String mode;
    private String baselineStartTime;
    private String baselineEndTime;
}
