package com.datamaster.module.ai.config;

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

    /** 单次模型调用超时时间，默认 3 分钟，适配本体大表结构生成。 */
    private Integer timeout = 180000;

    /** 超时/临时网络错误的自动重试次数。 */
    private Integer retryCount = 2;

    /** 自动重试间隔（毫秒）。 */
    private Long retryInterval = 1500L;

    private Double temperature = 0.2;

    private Integer maxTokens = 8192;
}

