package com.datamaster.module.ontology.controller.admin.analysis.vo;

import com.datamaster.module.ontology.controller.admin.metric.vo.MetricFilterVO;
import lombok.Data;
import javax.validation.Valid;
import javax.validation.constraints.*;
import java.util.ArrayList;
import java.util.List;

@Data
public class MetricQueryReqVO {
    @NotNull private Long metricId;
    @NotNull @Min(1) private Integer version;
    @Size(max = 5) private List<Long> dimensionPropertyIds = new ArrayList<>();
    @Valid @Size(max = 20) private List<MetricFilterVO> filters = new ArrayList<>();
    private String startTime;
    private String endTime;
    /** NONE/DAY/MONTH/YEAR in the registered source's business-local time. */
    private String timeBucket = "NONE";
    /** metric_value, d_<selected propertyId>, or time_bucket; null preserves legacy ordering. */
    private String sortBy;
    /** ASC/DESC, paired with sortBy. */
    private String sortDirection;
    /** Require one complete bounded result; overflow is an error, never a partial trend. */
    private Boolean requireComplete = false;
    @Min(1) @Max(1000) private Integer pageNum = 1;
    @Min(1) @Max(500) private Integer limit = 100;
}
