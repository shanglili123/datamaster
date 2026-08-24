package com.datamaster.module.ontology.controller.admin.ontology.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 本体保存 Request VO
 */
@Schema(description = "本体保存 Request VO")
@Data
public class OntologySaveReqVO {

    @Schema(description = "编号，更新时必填", example = "1")
    private Long id;

    @Schema(description = "本体名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "客户本体")
    @NotBlank(message = "本体名称不能为空")
    @Size(max = 100, message = "本体名称长度不能超过100个字符")
    private String name;

    @Schema(description = "本体编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "customer")
    @NotBlank(message = "本体编码不能为空")
    @Size(max = 100, message = "本体编码长度不能超过100个字符")
    private String code;

    @Schema(description = "描述", example = "客户域本体模型")
    private String description;

    @Schema(description = "所属空间ID", example = "1")
    private Long spaceId;

    @Schema(description = "空间编码", example = "space01")
    @Size(max = 50, message = "空间编码长度不能超过50个字符")
    private String spaceCode;

    @Schema(description = "状态：0=草稿 1=已发布 2=已归档", example = "0")
    private Integer status;
}
