package com.datamaster.module.ai.model.dto.dbgpt;

import lombok.Data;

import java.util.Map;

@Data
public class DbGptDatasourceCreateRequest {

    private Integer id;

    private String type;

    private Map<String, Object> params;

    private String description;
}

