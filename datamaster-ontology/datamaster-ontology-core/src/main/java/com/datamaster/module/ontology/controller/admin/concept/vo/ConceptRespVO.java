package com.datamaster.module.ontology.controller.admin.concept.vo;

import com.datamaster.common.annotation.Excel;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 本体概念 Response VO
 */
@Schema(description = "本体概念 Response VO")
@Data
public class ConceptRespVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "编号", example = "1")
    @Excel(name = "编号")
    private Long id;

    @Schema(description = "所属本体ID", example = "1")
    @Excel(name = "所属本体ID")
    private Long ontologyId;

    @Schema(description = "概念名称", example = "客户")
    @Excel(name = "概念名称")
    private String name;

    @Schema(description = "概念编码", example = "customer")
    @Excel(name = "概念编码")
    private String code;

    @Schema(description = "描述", example = "客户概念定义")
    @Excel(name = "描述")
    private String description;

    @Schema(description = "图标标识（可视化用）", example = "user")
    @Excel(name = "图标")
    private String icon;

    @Schema(description = "颜色（可视化用）", example = "#409EFF")
    @Excel(name = "颜色")
    private String color;

    @Schema(description = "排序", example = "1")
    @Excel(name = "排序")
    private Integer sortOrder;

    @Schema(description = "状态：0=草稿 1=已发布", example = "0")
    @Excel(name = "状态", readConverterExp = "0=草稿,1=已发布")
    private Integer status;

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
