package com.datamaster.module.ai.model.dto.dbgpt;

import com.alibaba.fastjson2.annotation.JSONField;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Map;

@Data
public class DbGptDatasourceResponse {

    private Integer id;
    private String type;
    private Map<String, Object> params;
    private String description;
    @JSONField(name = "db_name")
    @JsonProperty("db_name")
    private String dbName;
}

