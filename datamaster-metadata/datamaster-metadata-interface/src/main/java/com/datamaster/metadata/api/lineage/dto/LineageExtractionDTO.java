package com.datamaster.metadata.api.lineage.dto;

import lombok.Data;
import java.io.Serializable;
import java.util.List;

@Data
public class LineageExtractionDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    private String status;
    private String errorCode;
    private List<LineageRelationDTO> relations;
}
