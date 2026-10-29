package com.datamaster.module.ontology.api.dto;

import lombok.Data;
import java.util.List;

/** Model-authored schema draft; all physical bindings must still be validated by the ontology service. */
@Data
public class OntologyModelOutput {
    private String ontologyName;
    private String ontologyCode;
    private String ontologyDescription;
    private List<Concept> concepts;
    private List<Relation> relations;

    @Data
    public static class Concept {
        private String name;
        private String code;
        private String tableName;
        private String description;
        private String icon;
        private String color;
        private List<Property> properties;
    }
    @Data
    public static class Property {
        private String name;
        private String code;
        private String dataType;
        private String description;
        private Boolean isPrimary;
        private Boolean isRequired;
        private String defaultValue;
        private String columnName;
    }
    @Data
    public static class Relation {
        private String name;
        private String code;
        private String sourceConceptCode;
        private String targetConceptCode;
        private String relationType;
        private String description;
        private String sourceColumn;
        private String targetColumn;
    }
}
