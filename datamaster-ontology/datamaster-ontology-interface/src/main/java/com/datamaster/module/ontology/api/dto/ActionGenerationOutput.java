package com.datamaster.module.ontology.api.dto;

import lombok.Data;
import java.util.List;

/** Generated definitions are previews only, never authorization to execute. */
@Data
public class ActionGenerationOutput {
    private List<Action> actions;
    private String qualityWarning;

    @Data
    public static class Action {
        private String actionName;
        private String actionDescription;
        private String actionType;
        private String triggerConceptCode;
        private List<Step> executionSteps;
        private String functionBody;
        private String functionLang;
        private List<String> functionParams;
        private String sourceConceptCode;
        private String outputConceptCode;
    }
    @Data
    public static class Step {
        private String targetType;
        private String conceptCode;
        private String relationCode;
        private String actionType;
        private List<Parameter> paramConfig;
        private List<Condition> conditionConfig;
    }
    @Data
    public static class Parameter {
        private String propertyCode;
        private String relationEndpoint;
        private String valueMode;
        private String valueTemplate;
        private Boolean required;
    }
    @Data
    public static class Condition {
        private String propertyCode;
        private String relationEndpoint;
        private String operator;
        private Boolean negate;
        private String sourceType;
        private String sourcePropertyCode;
    }
}
