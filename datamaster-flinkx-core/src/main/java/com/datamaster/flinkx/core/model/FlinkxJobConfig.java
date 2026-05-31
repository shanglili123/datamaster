package com.datamaster.flinkx.core.model;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class FlinkxJobConfig {
    private String jobName;
    private String jobType;
    private String mode;
    private String schedulerName;
    private Map<String, Object> flinkx;
}
