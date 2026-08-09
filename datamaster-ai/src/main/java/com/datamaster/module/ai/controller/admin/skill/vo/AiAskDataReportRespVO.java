package com.datamaster.module.ai.controller.admin.skill.vo;

import com.alibaba.fastjson2.JSONObject;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * Ask-data report generation response.
 */
@Schema(description = "问数报告生成响应")
@Data
public class AiAskDataReportRespVO {

    private String question;

    private Long skillId;

    private Long templateId;

    private String templateCode;

    private String templateName;

    private String templateContent;

    private JSONObject reportData;

    private String sql;

    private String rawReply;

    private String qualityWarning;
}

