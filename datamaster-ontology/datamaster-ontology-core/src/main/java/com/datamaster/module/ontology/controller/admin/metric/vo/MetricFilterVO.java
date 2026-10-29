package com.datamaster.module.ontology.controller.admin.metric.vo;

import lombok.Data;

@Data
public class MetricFilterVO {
    private Long propertyId;
    private String operator;
    private Object value;
}
