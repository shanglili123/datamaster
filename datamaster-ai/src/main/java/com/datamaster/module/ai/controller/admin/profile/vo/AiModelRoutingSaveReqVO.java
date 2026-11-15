package com.datamaster.module.ai.controller.admin.profile.vo;

import lombok.Data;
import javax.validation.constraints.*;

@Data
public class AiModelRoutingSaveReqVO {
    @NotNull private Long spaceId;
    private String spaceCode;
    @NotNull private Boolean enabled;
    private Long economyProfileId;
    private Long advancedProfileId;
    @NotNull private Boolean semanticDecisionEnabled;
    private Long decisionProfileId;
    @NotNull private Boolean strictJsonSchema;
    @NotNull @Min(1) @Max(16000) private Integer complexInputCharacters;
    @NotNull @Min(0) private Integer revision;
}
