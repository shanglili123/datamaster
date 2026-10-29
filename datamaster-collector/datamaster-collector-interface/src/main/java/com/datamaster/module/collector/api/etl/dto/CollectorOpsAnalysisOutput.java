package com.datamaster.module.collector.api.etl.dto;

import lombok.Data;

/** AI classification is advisory; task recovery still requires the existing deterministic policy. */
@Data
public class CollectorOpsAnalysisOutput {
    public enum FailureType { NETWORK_OR_DATASOURCE, CODE_OR_RULE, PERMISSION, RESOURCE, DATA_QUALITY, SCHEDULER_OR_ENV, UNKNOWN }
    public enum RiskLevel { LOW, MEDIUM, HIGH }
    private FailureType failureType;
    private RiskLevel riskLevel;
    private Boolean recoverable;
    private String reason;
    private String suggestion;
}
