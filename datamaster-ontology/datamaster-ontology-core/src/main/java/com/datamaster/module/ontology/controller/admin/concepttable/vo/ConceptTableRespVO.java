package com.datamaster.module.ontology.controller.admin.concepttable.vo;

import com.datamaster.common.annotation.Excel;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Schema(description = "概念表绑定 Response VO")
@Data
public class ConceptTableRespVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "编号", example = "1")
    @Excel(name = "编号")
    private Long id;

    @Schema(description = "概念ID", example = "1")
    @Excel(name = "概念ID")
    private Long conceptId;

    @Schema(description = "数据源ID", example = "1")
    @Excel(name = "数据源ID")
    private Long datasourceId;

    @Schema(description = "数据库名", example = "mydb")
    @Excel(name = "数据库名")
    private String databaseName;

    @Schema(description = "表名", example = "t_customer")
    @Excel(name = "表名")
    private String tableName;

    @Schema(description = "Schema名", example = "public")
    @Excel(name = "Schema")
    private String schemaName;

    @Schema(description = "创建者", example = "admin")
    @Excel(name = "创建者")
    private String createBy;

    @Schema(description = "创建时间", example = "2026-08-22 10:00:00")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "创建时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;
}
