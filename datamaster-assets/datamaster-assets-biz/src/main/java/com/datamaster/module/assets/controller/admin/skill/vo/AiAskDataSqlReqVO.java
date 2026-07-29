package com.datamaster.module.assets.controller.admin.skill.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * Ask-data SQL generation request.
 */
@Schema(description = "问数SQL生成请求")
@Data
public class AiAskDataSqlReqVO {

    @Schema(description = "用户问题", requiredMode = Schema.RequiredMode.REQUIRED, example = "查询最近7天的订单总数")
    @NotBlank(message = "用户问题不能为空")
    private String question;

    @Schema(description = "资产ID（可选，用于聚焦特定表）", example = "123")
    private Long assetId;

    @Schema(description = "数据源ID（可选，用于AI问数）", example = "12")
    private Long datasourceId;

    @Schema(description = "空间ID", example = "1")
    private Long spaceId;

    @Schema(description = "空间编码", example = "174954643786848")
    private String spaceCode;

    @Schema(description = "模型名称（可选，默认使用配置文件模型）", example = "qwen-plus")
    private String model;

    @Schema(description = "AI问数模式", example = "chat_with_db_qa")
    private String chatMode;

    @Schema(description = "Skill ID列表（可选，指定使用的Skill）")
    private java.util.List<Long> skillIds;

    @Schema(description = "是否要求返回SQL用于校验", example = "false")
    private Boolean returnSql;
}
