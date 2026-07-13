package com.datamaster.module.assets.controller.admin.skill.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Schema(description = "AI问数消息窗口响应")
@Data
public class AiAskMessageWindowRespVO {

    private List<AiAskMessageRespVO> rows;

    private Boolean hasBefore;

    private Boolean hasAfter;
}
