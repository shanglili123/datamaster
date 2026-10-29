package com.datamaster.module.ontology.dal.dataobject;

import com.baomidou.mybatisplus.annotation.TableName;
import com.datamaster.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ont_metric_version")
public class MetricVersionDO extends BaseEntity {
    private Long metricId;
    private Integer version;
    private String snapshotJson;
}
