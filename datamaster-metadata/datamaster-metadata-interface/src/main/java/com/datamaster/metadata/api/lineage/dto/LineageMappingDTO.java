package com.datamaster.metadata.api.lineage.dto;

import lombok.Data;
import java.io.Serializable;

@Data
public class LineageMappingDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    private LineageEndpointDTO source;
    private LineageColumnDTO sourceColumn;
    private LineageColumnDTO targetColumn;
}
