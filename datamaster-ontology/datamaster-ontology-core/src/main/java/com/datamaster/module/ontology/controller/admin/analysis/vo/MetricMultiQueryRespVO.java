package com.datamaster.module.ontology.controller.admin.analysis.vo;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class MetricMultiQueryRespVO {
    private List<MetricQueryRespVO> metrics;
    private List<MetricQueryColumnVO> columns;
    private List<Map<String,Object>> rows;
    private String derivedOperation;
    private String derivedUnit;
    private String notice;
}
