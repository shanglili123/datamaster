package com.datamaster.module.assets.model.dto.dbgpt;

import com.alibaba.fastjson2.annotation.JSONField;
import lombok.Data;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
public class DbGptChatCompletionRequest {

    private String model;

    private List<DbGptChatMessage> messages;

    private Double temperature = 0.1;

    @JSONField(name = "max_tokens")
    private Integer maxTokens = 4096;

    private Boolean stream = false;

    @JSONField(name = "chat_mode")
    private String chatMode;

    @JSONField(name = "chat_param")
    private String chatParam;

    @JSONField(name = "space_name")
    private String spaceName;

    @JSONField(name = "datasource_id")
    private Integer datasourceId;

    @JSONField(name = "db_name")
    private String dbName;

    @JSONField(name = "db_type")
    private String dbType;

    @JSONField(name = "conv_uid")
    private String convUid;

    private Map<String, Object> extra = new LinkedHashMap<>();
}
