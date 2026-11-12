package com.datamaster.module.governance.api.flow;

import lombok.Data;
import java.util.ArrayList;
import java.util.List;

/** Read-only consumer inventory. No source SQL, credentials or data rows in this contract. */
public interface SpaceFlowImpactProvider {
    String type();
    Page inventory(Long spaceId, String spaceCode, int page, int pageSize);

    @Data
    class Consumer {
        private Long id;
        private String name;
        private String status;
        private Long datasourceId;
        private String tableName;
        private String sourceCoverage;
        private String notice;
        /** Preview facts only, never an execution permit or replacement for user/column authorization. */
        private String readiness;
        private List<String> missingRequirements = new ArrayList<>();
        private List<String> nextSteps = new ArrayList<>();
        private List<String> pendingChecks = new ArrayList<>();
    }
    @Data
    class Page {
        private long total;
        private List<Consumer> rows = new ArrayList<>();
    }
}
