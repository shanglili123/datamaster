package com.datamaster.metadata.api.lineage.dto;

import lombok.Data;
import java.io.Serializable;

@Data
public class LineageSyncResultDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    private String status;
    private String errorCode;
    private Long sequence;
}
