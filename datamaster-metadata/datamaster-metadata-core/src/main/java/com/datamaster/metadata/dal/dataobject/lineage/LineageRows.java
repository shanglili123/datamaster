package com.datamaster.metadata.dal.dataobject.lineage;

import lombok.Data;

/** New JDBC lineage rows are deliberately independent of legacy Neo4j entities. */
public final class LineageRows {
    private LineageRows() { }

    @Data
    public static class ObjectRow {
        private String id;
        private String kind;
        private Long datasourceId;
        private String catalogName;
        private String schemaName;
        private String objectName;
        private String externalKey;
        private String columnName;
        private String parentId;
    }

    @Data
    public static class ProducerRow {
        private String id;
        private Long taskId;
        private Long spaceId;
        private String spaceCode;
        private String taskName;
        private String taskType;
        private String taskCode;
        private Long sequence;
        private String version;
        private String status;
        private String errorCode;
        private String objectIdsJson;
        private Boolean active;
        private Boolean deleted;
        private Boolean stale;
        private Long lastgoodSequence;
    }

    @Data
    public static class EdgeRow {
        private String id;
        private String producerId;
        private Long sequence;
        private String sourceId;
        private String targetId;
        private String sourceColumnId;
        private String targetColumnId;
        private String unitKey;
        private String routeKey;
        private String kind;
        private String confidence;
        private Long taskId;
        private String taskName;
        private String status;
        private String errorCode;
        private Boolean stale;
        private Boolean active;
    }
}
