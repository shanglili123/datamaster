package com.datamaster.module.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/** Administrator-configured profile IDs only; clients/model output cannot choose arbitrary endpoints. */
@Data
@Component
@ConfigurationProperties(prefix = "ai.model-routing")
public class AiModelRoutingProperties {
    private boolean enabled;
    private int complexInputCharacters = 12000;
    private Map<Long, SpaceRoutes> spaces = new LinkedHashMap<>();

    @Data
    public static class SpaceRoutes {
        private Long economyProfileId;
        private Long advancedProfileId;
        /** Explicit opt-in: semantic classification adds a measured model request. */
        private boolean semanticDecisionEnabled;
        /** Null uses this space's economy model; never recursively route the classifier. */
        private Long decisionProfileId;
        private boolean strictJsonSchema;
    }
}
