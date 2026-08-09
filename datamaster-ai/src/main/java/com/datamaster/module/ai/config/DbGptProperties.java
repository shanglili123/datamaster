package com.datamaster.module.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "db-gpt")
public class DbGptProperties {

    private String url = "http://localhost:5670";

    private Integer timeout = 60000;

    private String model = "qwen-plus";

    private String chatMode = "chat_with_db_qa";

    private String skillSpaceName = "datamaster-skills";

    private String skillSpaceOwner = "datamaster";
}

