package com.datamaster.module.ai.controller.admin.skill.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

/**
 * AI Skill response.
 */
@Schema(description = "AI Skill response")
@Data
public class AiSkillRespVO {

    private Long id;

    private String skillCode;

    private String skillName;

    private String skillType;

    private String status;

    private String sourceType;

    private String bizObjectType;

    private Long bizObjectId;

    private String content;

    private String contentHash;

    private Integer version;

    private String dbgptSpaceName;

    private String dbgptDocumentName;

    private String dbgptSyncStatus;

    private String dbgptSyncMessage;

    private String createBy;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    private String updateBy;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;
}

