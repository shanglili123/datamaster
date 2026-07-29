package com.datamaster.module.assets.controller.admin.skill.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

/**
 * AI Skill report template response.
 */
@Schema(description = "AI Skill report template response")
@Data
public class AiSkillReportTemplateRespVO {

    private Long id;

    private Long skillId;

    private String templateCode;

    private String templateName;

    private String templateContent;

    private String status;

    private Boolean defaultFlag;

    private Integer version;

    private String description;

    private String createBy;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    private String updateBy;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;
}
