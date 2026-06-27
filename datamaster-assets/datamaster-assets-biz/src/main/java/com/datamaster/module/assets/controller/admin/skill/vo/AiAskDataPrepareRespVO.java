package com.datamaster.module.assets.controller.admin.skill.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * Ask-data prepare response.
 */
@Schema(description = "Ask-data prepare response")
@Data
public class AiAskDataPrepareRespVO {

    private String question;

    private List<AiSkillRespVO> skills;

    private String promptContext;
}
