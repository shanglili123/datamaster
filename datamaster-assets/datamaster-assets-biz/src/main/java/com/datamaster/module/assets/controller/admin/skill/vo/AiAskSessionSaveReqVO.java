package com.datamaster.module.assets.controller.admin.skill.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "AI问数会话保存请求")
@Data
public class AiAskSessionSaveReqVO {

    private Long spaceId;

    private String spaceCode;

    private String title;

    private String mode;

    private Long datasourceId;

    private String datasourceName;

    private Long skillId;

    private Long templateId;

    private Boolean returnSql;
}
