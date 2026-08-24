package com.datamaster.module.ontology.controller.admin.concept.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * 本体概念保存 Request VO
 */
@Schema(description = "本体概念保存 Request VO")
@Data
public class ConceptSaveReqVO {

    @Schema(description = "编号，更新时必填", example = "1")
    private Long id;

    @Schema(description = "所属本体ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "所属本体不能为空")
    private Long ontologyId;

    @Schema(description = "概念名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "客户")
    @NotBlank(message = "概念名称不能为空")
    @Size(max = 100, message = "概念名称长度不能超过100个字符")
    private String name;

    @Schema(description = "概念编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "customer")
    @NotBlank(message = "概念编码不能为空")
    @Size(max = 100, message = "概念编码长度不能超过100个字符")
    private String code;

    @Schema(description = "描述", example = "客户概念定义")
    private String description;

    @Schema(description = "图标标识（可视化用）", example = "user")
    @Size(max = 50, message = "图标标识长度不能超过50个字符")
    private String icon;

    @Schema(description = "颜色（可视化用）", example = "#409EFF")
    @Size(max = 20, message = "颜色长度不能超过20个字符")
    private String color;

    @Schema(description = "排序", example = "1")
    private Integer sortOrder;

    @Schema(description = "状态：0=草稿 1=已发布", example = "0")
    private Integer status;
}
