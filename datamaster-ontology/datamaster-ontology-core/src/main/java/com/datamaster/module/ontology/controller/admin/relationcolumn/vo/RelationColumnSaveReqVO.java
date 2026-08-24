package com.datamaster.module.ontology.controller.admin.relationcolumn.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Schema(description = "关系字段绑定保存 Request VO")
@Data
public class RelationColumnSaveReqVO {

    @Schema(description = "编号，更新时必填", example = "1")
    private Long id;

    @Schema(description = "关系ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "关系ID不能为空")
    private Long relationId;

    @Schema(description = "源概念表绑定ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "源概念表绑定ID不能为空")
    private Long sourceConceptTableId;

    @Schema(description = "源物理列名", requiredMode = Schema.RequiredMode.REQUIRED, example = "id")
    @NotBlank(message = "源物理列名不能为空")
    private String sourceColumn;

    @Schema(description = "目标概念表绑定ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "2")
    @NotNull(message = "目标概念表绑定ID不能为空")
    private Long targetConceptTableId;

    @Schema(description = "目标物理列名", requiredMode = Schema.RequiredMode.REQUIRED, example = "customer_id")
    @NotBlank(message = "目标物理列名不能为空")
    private String targetColumn;
}
