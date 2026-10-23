package com.datamaster.metadata.api.lineage.dto;

import lombok.Data;
import java.io.Serializable;

/** Structured identity. Never match tables by host or display SQL. */
@Data
public class LineageEndpointDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long datasourceId;
    private String kind;
    private String catalog;
    private String schema;
    private String table;
    private String externalKey;
    private String caseRule;
    private Boolean catalogQuoted;
    private Boolean schemaQuoted;
    private Boolean tableQuoted;
}
