package com.datamaster.module.ontology.api.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/** 本体决策 Skill 所需的稳定、只读语义上下文。 */
@Data
public class OntologyDecisionContextDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long ontologyId;
    private String ontologyName;
    private String ontologyCode;
    private String ontologyDescription;
    /** 本体绑定的真实数据源。 */
    private List<DatasourceDTO> datasources;
    private List<ConceptDTO> concepts;
    private List<RelationDTO> relations;
    private List<ActionDTO> actions;

    @Data
    public static class ConceptDTO implements Serializable {
        private Long id;
        private String name;
        private String code;
        private String description;
        private List<PropertyDTO> properties;
        /** 概念绑定的物理表。 */
        private List<TableBindingDTO> tableBindings;
    }

    @Data
    public static class PropertyDTO implements Serializable {
        private Long id;
        private String name;
        private String code;
        private String dataType;
        private String description;
        private Boolean primary;
        private Boolean required;
        /** 属性对应的物理字段。 */
        private List<ColumnBindingDTO> columnBindings;
    }

    @Data
    public static class RelationDTO implements Serializable {
        private Long id;
        private String name;
        private String code;
        private String relationType;
        private Long sourceConceptId;
        private String sourceConceptName;
        private String sourceConceptCode;
        private Long targetConceptId;
        private String targetConceptName;
        private String targetConceptCode;
        private String description;
        /** 关系绑定的物理关系表。 */
        private List<TableBindingDTO> tableBindings;
        /** 关系两端的物理关联字段。 */
        private List<RelationColumnBindingDTO> columnBindings;
    }

    @Data
    public static class ActionDTO implements Serializable {
        private Long id;
        private String name;
        private String actionType;
        private Long triggerConceptId;
        private String description;
        private String paramConfig;
        private String executionSteps;
        private String submissionCriteria;
        private Boolean needsApproval;
        private Integer approvalLevels;
    }

    @Data
    public static class DatasourceDTO implements Serializable {
        private Long id;
        private String name;
        private String type;
        private String ip;
        private Long port;
    }

    @Data
    public static class TableBindingDTO implements Serializable {
        private Long id;
        private Long datasourceId;
        private String datasourceName;
        private String datasourceType;
        private String databaseName;
        private String schemaName;
        private String tableName;
        private String columnNames;
    }

    @Data
    public static class ColumnBindingDTO implements Serializable {
        private Long id;
        private Long propertyId;
        private Long conceptTableId;
        private Long datasourceId;
        private String datasourceName;
        private String datasourceType;
        private String databaseName;
        private String schemaName;
        private String tableName;
        private String columnName;
    }

    @Data
    public static class RelationColumnBindingDTO implements Serializable {
        private Long id;
        private Long relationId;
        private Long sourceConceptTableId;
        private String sourceColumn;
        private Long sourceDatasourceId;
        private String sourceDatasourceName;
        private String sourceDatabaseName;
        private String sourceSchemaName;
        private String sourceTableName;
        private Long targetConceptTableId;
        private String targetColumn;
        private Long targetDatasourceId;
        private String targetDatasourceName;
        private String targetDatabaseName;
        private String targetSchemaName;
        private String targetTableName;
    }
}
