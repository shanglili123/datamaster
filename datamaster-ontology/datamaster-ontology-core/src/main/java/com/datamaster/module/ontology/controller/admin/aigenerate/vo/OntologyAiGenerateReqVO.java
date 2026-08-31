package com.datamaster.module.ontology.controller.admin.aigenerate.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * 本体 AI 生成 Request VO
 */
@Schema(description = "本体 AI 生成 Request VO")
@Data
public class OntologyAiGenerateReqVO {

    @Schema(description = "数据源ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "数据源ID不能为空")
    private Long datasourceId;

    @Schema(description = "业务表名；为空则生成该数据源的全部表", example = "t_customer")
    private String tableName;

    @Schema(description = "目标本体ID；为空则 AI 生成新本体", example = "1")
    private Long ontologyId;

    @Schema(description = "所属空间ID", example = "1")
    private Long spaceId;

    @Schema(description = "空间编码", example = "space01")
    @Size(max = 50, message = "空间编码长度不能超过50个字符")
    private String spaceCode;

    @Schema(description = "模型名称，为空使用系统默认模型", example = "qwen-plus")
    private String model;
}