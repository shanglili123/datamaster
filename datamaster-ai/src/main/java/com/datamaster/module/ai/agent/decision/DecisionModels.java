package com.datamaster.module.ai.agent.decision;

import lombok.Data;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Decision artifacts are data, never executable instructions or authorization. */
public final class DecisionModels {
    private DecisionModels() { }

    @Data
    public static class Proposal {
        private String id;
        private String operationId;
        private String status;
        private String goal;
        private String ontologyId;
        private String objectKey;
        private String selectedCandidateId;
        private String previewDigest;
        private String definitionDigest;
        private String executionId;
        private String runId;
        private String executionStatus;
        private String message;
        private Integer revision;
        private List<Evidence> evidence = new ArrayList<>();
        private List<Candidate> candidates = new ArrayList<>();
        private Binding binding;
        private Date expiresAt;
        private String effectStatus = "UNKNOWN";
        private String effectMessage = "尚未复查业务效果；执行成功不代表目标改善";
        private CalculationContext calculationContext;
        private Comparison comparison;
    }

    @Data
    public static class Evidence {
        private String id;
        private String source;
        private String operationId;
        private String verification;
        private String summary;
        private Map<String, Object> details = new LinkedHashMap<>();
    }

    @Data
    public static class Candidate {
        private String id;
        private String name;
        private String rationale;
        private boolean baseline;
        private List<String> evidenceIds = new ArrayList<>();
        private List<String> risks = new ArrayList<>();
        private List<Constraint> constraints = new ArrayList<>();
        private List<Impact> impacts = new ArrayList<>();
        private Evaluation evaluation;
        private Binding suggestedBinding;
        private CalculationSpec calculationSpec;
        private CalculationResult calculation;
    }

    @Data
    public static class Constraint {
        private String name;
        private String status;
        private String reason;
    }

    @Data
    public static class Impact {
        private String name;
        private String value;
        private String unit;
        private String method;
        private String evidenceId;
    }

    @Data
    public static class Evaluation {
        private String status;
        private String summary;
        private List<String> unknowns = new ArrayList<>();
    }

    @Data
    public static class Binding {
        private String type;
        private String targetId;
        private String name;
        private String objectKey;
        private String inputParams;
        private String definitionDigest;
        private Integer version;
        private boolean requiresApproval;
    }

    @Data
    public static class Request {
        private String decisionId;
        private String candidateId;
        private String previewDigest;
        private Integer revision;
    }

    @Data
    public static class AnalysisInput {
        private String question;
        /** METRIC or EXPLORATORY; never a fallback from failed formal metrics. */
        private String source;
    }

    @Data
    public static class MeasureRef {
        /** USER or METRIC; USER values are assumptions, not measured facts. */
        private String source;
        private String value;
        private String evidenceId;
        private Integer toolIndex;
        private Integer rowIndex;
        private String columnKey;
        private String unit;
    }

    @Data
    public static class CalculationContext {
        private String scope;
        private String period;
        private String currency;
        private String quantityUnit;
        private String strategy;
        private Boolean noBudgetLimit;
        private MeasureRef demand;
        private MeasureRef existingSupply;
        private MeasureRef budget;
    }

    @Data
    public static class ResourceLine {
        private String name;
        private String inputUnit;
        private Boolean fractionalQuantity;
        private MeasureRef quantity;
        private MeasureRef unitPrice;
        private MeasureRef outputPerUnit;
        /** Top-level action/Workflow parameter corresponding to this line's quantity. */
        private String quantityParameter;
    }

    @Data
    public static class CalculationSpec {
        private String candidateId;
        private Boolean alignExecutionQuantities;
        private MeasureRef fixedCost;
        private List<ResourceLine> lines = new ArrayList<>();
    }

    @Data
    public static class CalculationRequest {
        private String decisionId;
        private Integer revision;
        private Boolean confirmedInputs;
        private CalculationContext context;
        private List<CalculationSpec> candidates = new ArrayList<>();
    }

    @Data
    public static class CalculationResult {
        private String status;
        private String totalCost;
        private String additionalSupply;
        private String availableSupply;
        private String remainingGap;
        private String coveragePercent;
        private String budgetStatus;
        private String inputDigest;
        private String bindingInputDigest;
        private String version = "COST_CAPACITY_V1";
        private Integer rank;
        private boolean recommended;
        private boolean conditional = true;
        private Map<String, String> inputs = new LinkedHashMap<>();
        private Map<String, String> sources = new LinkedHashMap<>();
        private Map<String, String> executionQuantities = new LinkedHashMap<>();
        private List<String> limitations = new ArrayList<>();
        private String formula;
        private String confirmedBy;
        private Date confirmedAt;
    }

    @Data
    public static class Comparison {
        private String status;
        private String strategy;
        private String summary;
        private List<String> recommendedIds = new ArrayList<>();
        private List<String> excludedIds = new ArrayList<>();
        private List<String> limitations = new ArrayList<>();
    }

    @Data
    public static class PlanningInput {
        private String instructions;
    }
}
