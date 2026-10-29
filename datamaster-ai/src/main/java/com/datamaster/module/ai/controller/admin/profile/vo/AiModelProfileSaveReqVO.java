package com.datamaster.module.ai.controller.admin.profile.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.alibaba.fastjson2.annotation.JSONField;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/** Write-only credentials; deliberately has no generated toString. */
@Getter
@Setter
public class AiModelProfileSaveReqVO {
    private Long spaceId;
    @Size(max = 128)
    private String spaceCode;
    @NotBlank @Size(max = 100)
    private String name;
    @NotBlank @Size(max = 64)
    private String provider;
    @NotBlank @Size(max = 100)
    private String model;
    @NotBlank @Size(max = 500)
    private String apiUrl;
    @NotNull
    private Boolean enabled;
    /** Blank preserves the stored credential on update; required on create. */
    @Size(max = 8192)
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @JSONField(serialize = false)
    private String apiKey;
}
