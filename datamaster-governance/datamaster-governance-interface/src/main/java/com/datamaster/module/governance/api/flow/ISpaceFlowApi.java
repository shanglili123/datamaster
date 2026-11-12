package com.datamaster.module.governance.api.flow;

import lombok.Data;
import java.util.*;

/** Published space rules, not UI visibility and not warehouse layer conventions. */
public interface ISpaceFlowApi {
    Policy policy(Long spaceId, String spaceCode);
    void assertQuality(Long spaceId, String spaceCode, Long datasourceId, String tableName);

    @Data
    class Policy {
        private boolean configured;
        private int version;
        private boolean assetRequired;
        private boolean qualityRequired;
    }
    @Data
    class Definition {
        private List<String> nodes = new ArrayList<>();
        private List<Edge> edges = new ArrayList<>();
        /** Presentation-only positions; never interpreted as execution order or authority. */
        private Map<String, Position> layout = new LinkedHashMap<>();
    }
    @Data
    class Position { private Double x; private Double y; }
    @Data
    class Edge { private String from; private String to; }
    @Data
    class SaveRequest {
        private Long spaceId;
        private String spaceCode;
        private Integer revision;
        private Definition definition;
    }
    @Data
    class QualityRequest {
        private Long spaceId;
        private String spaceCode;
        private Long datasourceId;
        private String tableName;
        private Long qualityLogId;
        private boolean manual;
        private boolean revoke;
        private String reason;
    }
}
