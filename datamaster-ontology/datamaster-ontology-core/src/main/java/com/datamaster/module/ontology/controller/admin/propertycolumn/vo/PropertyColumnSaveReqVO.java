package com.datamaster.module.ontology.controller.admin.propertycolumn.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Schema(description = "属性字段绑定保存 Request VO")
@Data
public class PropertyColumnSaveReqVO {

    @Schema(description = "编号，更新时必填", example = "1")
    private Long id;

    @Schema(description = "属性ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "属性ID不能为空")
    private Long propertyId;

    @Schema(description = "概念表绑定ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "概念表绑定ID不能为空")
    private Long conceptTableId;

    @Schema(description = "物理列名", requiredMode = Schema.RequiredMode.REQUIRED, example = "customer_name")
    @NotBlank(message = "物理列名不能为空")
    private String columnName;
}
