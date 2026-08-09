package com.datamaster.module.ai.controller.admin.skill.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "AI问数消息保存请求")
@Data
public class AiAskMessageSaveReqVO {

    private String role;

    private String content;

    private String displayContent;

    private String payloadJson;
}

