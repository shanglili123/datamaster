package com.datamaster.module.ai.controller.admin.skill.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

@Schema(description = "决策智能体会话响应")
@Data
public class AiAskSessionRespVO {

    private Long id;

    private Long userId;

    private Long spaceId;

    private String spaceCode;

    private String title;

    private String mode;

    private Long datasourceId;

    private String datasourceName;

    private Long skillId;

    private Long templateId;

    private Boolean returnSql;

    private Integer messageCount;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;
}

