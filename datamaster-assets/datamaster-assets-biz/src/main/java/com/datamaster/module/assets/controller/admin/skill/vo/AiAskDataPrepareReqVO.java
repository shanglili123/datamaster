package com.datamaster.module.assets.controller.admin.skill.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * Ask-data prepare request.
 */
@Schema(description = "Ask-data prepare request")
@Data
public class AiAskDataPrepareReqVO {

    private String question;

    private Long assetId;

    private Long projectId;

    private String projectCode;

    private String keyword;
}
