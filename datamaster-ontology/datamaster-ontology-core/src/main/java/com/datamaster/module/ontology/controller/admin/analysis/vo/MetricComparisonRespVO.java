package com.datamaster.module.ontology.controller.admin.analysis.vo;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class MetricComparisonRespVO {
    private MetricQueryRespVO current;
    private MetricQueryRespVO baseline;
    private String mode;
    private String baselineStartTime;
    private String baselineEndTime;
    private List<MetricQueryColumnVO> columns;
    private List<Map<String,Object>> rows;
    private boolean grouped;
    private String currentStartTime;
    private String currentEndTime;
    private String difference;
    private String changePercent;
    private String notice;
}
