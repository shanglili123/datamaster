package com.datamaster.metadata.api.lineage.dto;

import lombok.Data;
import java.io.Serializable;

@Data
public class LineageColumnDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    private String name;
    private Boolean quoted;
}
