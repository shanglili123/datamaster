package com.datamaster.module.assets.controller.admin.skill.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

/**
 * AI Skill version response.
 */
@Schema(description = "AI Skill version response")
@Data
public class AiSkillVersionRespVO {

    private Long id;

    private Long skillId;

    private Integer version;

    private String content;

    private String changeType;

    private String changeRemark;

    private String createBy;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;
}
