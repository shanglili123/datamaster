package com.datamaster.metadata.api.lineage.dto;

import lombok.Data;
import java.io.Serializable;
import java.util.List;

@Data
public class LineageGraphDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    private String centerId;
    private String status;
    private List<LineageNodeDTO> nodes;
    private List<LineageEdgeDTO> edges;
    private List<String> diagnostics;
    private Boolean truncated;
    private Integer depth;
}
