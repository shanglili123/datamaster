package com.datamaster.module.collector.utils.model;

import lombok.Builder;
import lombok.Data;

/**
 * Incremental FLINKX task metadata persisted with the collector task.
 */
@Data
@Builder
public class FlinkxIncrementalConfig {

    public static final String TYPE_ID = "ID";
    public static final String TYPE_TIME = "TIME";
    public static final String DEFAULT_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss.SSSSSS";

    private String incrementalType;
    private Long sourceDatasourceId;
    private Long targetDatasourceId;
    private String sourceTableName;
    private String targetTableName;
    private String sourceIncrementColumn;
    private String targetIncrementColumn;
    private String incrementalInitialValue;
    private String incrementalTimeFormat;
}
