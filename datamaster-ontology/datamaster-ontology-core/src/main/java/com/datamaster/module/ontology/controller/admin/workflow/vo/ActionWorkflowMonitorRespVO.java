package com.datamaster.module.ontology.controller.admin.workflow.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Workflow 运行监控快照。仅统计当前用户有权访问的最近运行样本。 */
@Data
public class ActionWorkflowMonitorRespVO {
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date observedAt;
    private Integer sampleSize;
    private Map<String, Long> counts = new LinkedHashMap<>();
    private List<ActionWorkflowRunRespVO> recentRuns;
}
