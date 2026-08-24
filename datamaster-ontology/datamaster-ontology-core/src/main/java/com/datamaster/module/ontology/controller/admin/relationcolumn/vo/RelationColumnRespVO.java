package com.datamaster.module.ontology.controller.admin.relationcolumn.vo;

import com.datamaster.common.annotation.Excel;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Schema(description = "关系字段绑定 Response VO")
@Data
public class RelationColumnRespVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "编号", example = "1")
    @Excel(name = "编号")
    private Long id;

    @Schema(description = "关系ID", example = "1")
    @Excel(name = "关系ID")
    private Long relationId;

    @Schema(description = "源概念表绑定ID", example = "1")
    @Excel(name = "源概念表绑定ID")
    private Long sourceConceptTableId;

    @Schema(description = "源物理列名", example = "id")
    @Excel(name = "源物理列名")
    private String sourceColumn;

    @Schema(description = "目标概念表绑定ID", example = "2")
    @Excel(name = "目标概念表绑定ID")
    private Long targetConceptTableId;

    @Schema(description = "目标物理列名", example = "customer_id")
    @Excel(name = "目标物理列名")
    private String targetColumn;

    @Schema(description = "创建者", example = "admin")
    @Excel(name = "创建者")
    private String createBy;

    @Schema(description = "创建时间", example = "2026-08-22 10:00:00")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "创建时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;
}
