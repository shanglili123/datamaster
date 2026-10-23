package com.datamaster.metadata.api.lineage.dto;

import lombok.Data;
import java.io.Serializable;

@Data
public class LineageQueryReqDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    private String objectId;
    private Long spaceId;
    private String code;
    private String spaceCode;
    private String direction;
    private String granularity;
    private Integer depth;
    private Boolean includeInactive;
}
