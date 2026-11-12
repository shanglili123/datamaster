package com.datamaster.module.ontology.controller.admin.analysis.vo;

import lombok.Data;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Data
public class MetricQueryRespVO {
    private Long metricId;
    private Integer version;
    private String metricName;
    private String unit;
    private List<MetricQueryColumnVO> columns;
    private List<Map<String, Object>> rows;
    private Boolean truncated;
    private Date executedAt;
    private Long elapsedMs;
    private Integer pageNum;
    private Boolean hasMore;
    private Integer limit;
    private String sourceTimeOffset;
    private String businessTimeOffset;
    private String timeBucket;
    private String sortBy;
    private String sortDirection;
    private String notice;
}
