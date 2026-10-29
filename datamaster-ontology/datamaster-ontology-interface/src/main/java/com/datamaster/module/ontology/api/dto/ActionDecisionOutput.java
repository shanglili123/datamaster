package com.datamaster.module.ontology.api.dto;

import lombok.Data;
import java.util.List;
import java.util.Map;

/** Typed suggestion, not an approval. IDs remain exact strings across model boundaries. */
@Data
public class ActionDecisionOutput {
    public enum Decision { ALLOW, ASK_HUMAN, REJECT }
    private Decision decision;
    private String actionId;
    private Map<String, Object> inputParams;
    private String reason;
    private Double confidence;
    private List<String> missingFields;
}
