package com.datamaster.module.ontology.controller.admin.concepttable.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Schema(description = "概念表绑定保存 Request VO")
@Data
public class ConceptTableSaveReqVO {

    @Schema(description = "编号，更新时必填", example = "1")
    private Long id;

    @Schema(description = "概念ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "概念ID不能为空")
    private Long conceptId;

    @Schema(description = "数据源ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "数据源ID不能为空")
    private Long datasourceId;

    @Schema(description = "数据库名", example = "mydb")
    private String databaseName;

    @Schema(description = "表名", requiredMode = Schema.RequiredMode.REQUIRED, example = "t_customer")
    @NotBlank(message = "表名不能为空")
    private String tableName;

    @Schema(description = "Schema名", example = "public")
    private String schemaName;
}
