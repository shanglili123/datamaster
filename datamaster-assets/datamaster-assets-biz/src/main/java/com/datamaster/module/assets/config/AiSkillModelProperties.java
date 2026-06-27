package com.datamaster.module.assets.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Model configuration used by DataMaster to generate governable AI skills.
 */
@Data
@Component
@ConfigurationProperties(prefix = "ai.skill-model")
public class AiSkillModelProperties {

    private Boolean enabled = false;

    private String apiUrl;

    private String apiKey;

    private String model = "qwen-plus";

    private Integer timeout = 60000;

    private Double temperature = 0.2;

    private Integer maxTokens = 4096;
}
