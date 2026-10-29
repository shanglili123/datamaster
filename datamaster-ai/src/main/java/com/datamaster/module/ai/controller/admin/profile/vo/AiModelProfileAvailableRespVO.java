package com.datamaster.module.ai.controller.admin.profile.vo;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** Safe member-facing selector projection: no endpoint or credential fields. */
@Getter
@AllArgsConstructor
public class AiModelProfileAvailableRespVO {
    private final Long id;
    private final String name;
    private final String provider;
    private final String model;
}
