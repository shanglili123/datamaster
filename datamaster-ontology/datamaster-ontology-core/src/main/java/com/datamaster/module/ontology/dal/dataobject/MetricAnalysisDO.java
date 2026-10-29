package com.datamaster.module.ontology.dal.dataobject;

import com.baomidou.mybatisplus.annotation.TableName;
import com.datamaster.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** Saved request only; physical deletion, never results, SQL, or datasource credentials. */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ont_metric_analysis")
public class MetricAnalysisDO extends BaseEntity {
    private Long ontologyId;
    private Long ownerId;
    private String name;
    private String description;
    private Long metricId;
    private Integer version;
    private String queryJson;
    private Integer revision;
}
