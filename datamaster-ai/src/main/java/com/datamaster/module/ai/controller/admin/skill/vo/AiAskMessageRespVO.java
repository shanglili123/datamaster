package com.datamaster.module.ai.controller.admin.skill.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

@Schema(description = "AI问数消息响应")
@Data
public class AiAskMessageRespVO {

    private Long id;

    private Long sessionId;

    private String role;

    private String content;

    private String displayContent;

    private String payloadJson;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;
}

