package com.datamaster.module.ontology.controller.admin.relation.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * 本体关系保存 Request VO
 */
@Schema(description = "本体关系保存 Request VO")
@Data
public class RelationSaveReqVO {

    @Schema(description = "编号，更新时必填", example = "1")
    private Long id;

    @Schema(description = "所属本体ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "所属本体不能为空")
    private Long ontologyId;

    @Schema(description = "关系名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "拥有订单")
    @NotBlank(message = "关系名称不能为空")
    @Size(max = 100, message = "关系名称长度不能超过100个字符")
    private String name;

    @Schema(description = "关系编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "has_order")
    @NotBlank(message = "关系编码不能为空")
    @Size(max = 100, message = "关系编码长度不能超过100个字符")
    private String code;

    @Schema(description = "源概念ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "源概念不能为空")
    private Long sourceConceptId;

    @Schema(description = "目标概念ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "2")
    @NotNull(message = "目标概念不能为空")
    private Long targetConceptId;

    @Schema(description = "关系类型：one_to_one/one_to_many/many_to_one/many_to_many", example = "one_to_many")
    @Size(max = 20, message = "关系类型长度不能超过20个字符")
    private String relationType;

    @Schema(description = "描述", example = "客户与订单的一对多关系")
    private String description;

    @Schema(description = "排序", example = "1")
    private Integer sortOrder;
}
