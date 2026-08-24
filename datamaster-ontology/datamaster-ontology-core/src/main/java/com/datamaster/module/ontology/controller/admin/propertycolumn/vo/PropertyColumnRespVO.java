package com.datamaster.module.ontology.controller.admin.propertycolumn.vo;

import com.datamaster.common.annotation.Excel;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Schema(description = "属性字段绑定 Response VO")
@Data
public class PropertyColumnRespVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "编号", example = "1")
    @Excel(name = "编号")
    private Long id;

    @Schema(description = "属性ID", example = "1")
    @Excel(name = "属性ID")
    private Long propertyId;

    @Schema(description = "概念表绑定ID", example = "1")
    @Excel(name = "概念表绑定ID")
    private Long conceptTableId;

    @Schema(description = "物理列名", example = "customer_name")
    @Excel(name = "物理列名")
    private String columnName;

    @Schema(description = "创建者", example = "admin")
    @Excel(name = "创建者")
    private String createBy;

    @Schema(description = "创建时间", example = "2026-08-22 10:00:00")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "创建时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;
}
