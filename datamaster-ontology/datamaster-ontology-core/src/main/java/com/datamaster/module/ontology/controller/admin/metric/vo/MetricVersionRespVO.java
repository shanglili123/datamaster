package com.datamaster.module.ontology.controller.admin.metric.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.util.Date;

@Data
public class MetricVersionRespVO {
    private Long id;
    private Long metricId;
    private Integer version;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") private Date createTime;
    private String createBy;
}
