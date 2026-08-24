package com.datamaster.module.ontology.controller.admin.relationtable.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Schema(description = "关系关联表绑定保存 Request VO")
@Data
public class RelationTableSaveReqVO {

    @Schema(description = "编号，更新时必填", example = "1")
    private Long id;

    @Schema(description = "关系ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "关系ID不能为空")
    private Long relationId;

    @Schema(description = "数据源ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "数据源ID不能为空")
    private Long datasourceId;

    @Schema(description = "数据库名", example = "mydb")
    private String databaseName;

    @Schema(description = "表名", requiredMode = Schema.RequiredMode.REQUIRED, example = "t_order")
    @NotBlank(message = "表名不能为空")
    private String tableName;

    @Schema(description = "Schema名", example = "public")
    private String schemaName;

    @Schema(description = "已选物理字段名, JSON数组", example = "[\"id\",\"order_no\"]")
    private String columnNames;
}
