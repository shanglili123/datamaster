package com.datamaster.module.ontology.controller.admin.property.vo;

import com.datamaster.common.annotation.Excel;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 本体属性 Response VO
 */
@Schema(description = "本体属性 Response VO")
@Data
public class PropertyRespVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "编号", example = "1")
    @Excel(name = "编号")
    private Long id;

    @Schema(description = "所属概念ID", example = "1")
    @Excel(name = "所属概念ID")
    private Long conceptId;

    @Schema(description = "属性名称", example = "客户名称")
    @Excel(name = "属性名称")
    private String name;

    @Schema(description = "属性编码", example = "customer_name")
    @Excel(name = "属性编码")
    private String code;

    @Schema(description = "数据类型：string/integer/decimal/date/boolean/text", example = "string")
    @Excel(name = "数据类型")
    private String dataType;

    @Schema(description = "描述", example = "客户全称")
    @Excel(name = "描述")
    private String description;

    @Schema(description = "是否主键属性", example = "false")
    @Excel(name = "是否主键属性", readConverterExp = "true=是,false=否")
    private Boolean isPrimary;

    @Schema(description = "是否必填", example = "false")
    @Excel(name = "是否必填", readConverterExp = "true=是,false=否")
    private Boolean isRequired;

    @Schema(description = "默认值", example = "未知")
    @Excel(name = "默认值")
    private String defaultValue;

    @Schema(description = "排序", example = "1")
    @Excel(name = "排序")
    private Integer sortOrder;

    @Schema(description = "创建者", example = "admin")
    @Excel(name = "创建者")
    private String createBy;

    @Schema(description = "创建时间", example = "2026-08-22 10:00:00")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "创建时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    @Schema(description = "更新者", example = "admin")
    @Excel(name = "更新者")
    private String updateBy;

    @Schema(description = "更新时间", example = "2026-08-22 10:00:00")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "更新时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;
}
