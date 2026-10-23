package com.datamaster.metadata.api.lineage.dto;

import lombok.Data;
import java.io.Serializable;
import java.util.List;

/** Monotonic task-producer snapshot; SQL is input-only and is never stored in the graph. */
@Data
public class LineageSnapshotDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long taskId;
    private Long spaceId;
    private Long sequence;
    private String spaceCode;
    private String taskName;
    private String taskType;
    private String taskCode;
    private String version;
    private String eventType;
    private String captureStatus;
    private String errorCode;
    private List<LineageUnitDTO> units;
}
