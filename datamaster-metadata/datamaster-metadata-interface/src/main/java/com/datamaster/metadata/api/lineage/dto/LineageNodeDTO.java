package com.datamaster.metadata.api.lineage.dto;

import lombok.Data;
import java.io.Serializable;

@Data
public class LineageNodeDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    private String id;
    private String type;
    private String name;
    private Long metadataId;
    private Long assetId;
    private Long datasourceId;
    private Long taskId;
    private Long spaceId;
    private String parentId;
    private String columnName;
    private String status;
    private Boolean stale;
}
