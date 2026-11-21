package com.datamaster.module.ai.controller.admin.skill.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;
import java.util.ArrayList;
import java.util.List;

/** Safe, credential-free context for ontology-first ask-data configuration. */
@Data
public class AiAskOntologyContextRespVO {
    @JsonSerialize(using = ToStringSerializer.class)
    private Long ontologyId;
    private List<Datasource> datasources = new ArrayList<>();
    private List<Metric> metrics = new ArrayList<>();

    @Data
    public static class Datasource {
        @JsonSerialize(using = ToStringSerializer.class)
        private Long id;
        private String datasourceName;
        private String dbgptSyncStatus;
    }

    @Data
    public static class Metric {
        @JsonSerialize(using = ToStringSerializer.class)
        private Long id;
        private String name;
        private Integer publishedVersion;
    }
}
