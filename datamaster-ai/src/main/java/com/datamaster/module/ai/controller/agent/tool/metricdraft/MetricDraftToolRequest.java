package com.datamaster.module.ai.controller.agent.tool.metricdraft;

import lombok.Data;
import java.util.ArrayList;
import java.util.List;

/** Concrete model schemas. IDs are strings; no SQL/column/definition write input is accepted. */
public final class MetricDraftToolRequest {
    private MetricDraftToolRequest() { }

    @Data
    public static class Preview {
        private String ontologyId;
        private String conceptId;
        private List<Candidate> suggestions;
    }

    @Data
    public static class Candidate {
        private String code;
        private String name;
        private String description;
        private String owner;
        private String unit;
        private String grain;
        private String aggregation;
        private String conceptTableId;
        private String valuePropertyId;
        private String timePropertyId;
        private List<String> dimensionPropertyIds = new ArrayList<>();
    }

    @Data
    public static class Save {
        private String previewOperationId;
        private Integer previewIndex;
        /** Public content digest, not a credential. Must come from preview_metrics. */
        private String previewDigest;
    }
}
