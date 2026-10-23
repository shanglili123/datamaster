package com.datamaster.metadata.api.lineage.dto;

import lombok.Data;
import java.io.Serializable;

@Data
public class LineageEdgeDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    private String id;
    private String source;
    private String target;
    private String kind;
    private String confidence;
    private String unitKey;
    private String routeKey;
    private String status;
    private Long taskId;
    private Boolean stale;
}
