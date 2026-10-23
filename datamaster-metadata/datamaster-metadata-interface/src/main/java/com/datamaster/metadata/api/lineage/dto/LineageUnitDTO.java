package com.datamaster.metadata.api.lineage.dto;

import lombok.Data;
import java.io.Serializable;
import java.util.List;

/** A single isolated reader/writer route. Column lists require reliable physical order. */
@Data
public class LineageUnitDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    private String unitKey;
    private String routeKey;
    private LineageEndpointDTO context;
    private List<LineageEndpointDTO> sources;
    private LineageEndpointDTO target;
    private List<LineageColumnDTO> sourceColumns;
    private List<LineageColumnDTO> targetColumns;
    private List<LineageColumnDTO> generatedColumns;
    private Boolean columnOrderReliable;
    private String readerSql;
    private String transformSql;
    private List<LineageMappingDTO> mappings;
}
