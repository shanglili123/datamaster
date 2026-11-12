package com.datamaster.module.ontology.controller.admin.analysis.vo;

import lombok.Data;
import javax.validation.Valid;
import javax.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;

@Data
public class MetricMultiQueryReqVO {
    @Valid @Size(min = 2, max = 4) private List<MetricQueryReqVO> queries = new ArrayList<>();
    /** NONE, RATIO, DIFFERENCE or SUM. Binary derivations use the first two selected metrics. */
    private String derivedOperation = "NONE";
}
