package com.datamaster.flinkx.core.model;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class FlinkxJobBundle {
    private FlinkxJobConfig jobConfig;
    private Map<String, Object> reader;
    private Map<String, Object> writer;
    private Object transitions;
}
