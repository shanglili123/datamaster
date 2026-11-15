package com.datamaster.module.ai.controller.admin.profile.vo;

import lombok.Data;

@Data
public class AiModelRoutingRespVO {
    private Long spaceId;
    private String spaceCode;
    private Boolean enabled;
    private Long economyProfileId;
    private Long advancedProfileId;
    private Boolean semanticDecisionEnabled;
    private Long decisionProfileId;
    private Boolean strictJsonSchema;
    private Integer complexInputCharacters;
    private Integer revision;
    private String source;
}
