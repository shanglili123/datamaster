package com.datamaster.module.ai.controller.admin.profile.vo;

import lombok.Getter;
import lombok.Setter;

/** Explicit management projection: never includes plaintext or encrypted credentials. */
@Getter
@Setter
public class AiModelProfileRespVO {
    private Long id;
    private Long spaceId;
    private String spaceCode;
    private String name;
    private String provider;
    private String model;
    private String apiUrl;
    private Boolean enabled;
    private Boolean active;
    private Boolean hasApiKey;
    private Boolean dbgptSyncEnabled;
    private String dbgptSyncStatus;
    private String dbgptSyncError;
    private String dbgptLastAttemptAt;
    private String dbgptRemoteAlias;
}
