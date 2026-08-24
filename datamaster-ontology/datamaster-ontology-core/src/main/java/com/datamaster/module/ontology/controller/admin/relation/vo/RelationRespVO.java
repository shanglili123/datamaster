package com.datamaster.module.ontology.controller.admin.relation.vo;

import com.datamaster.common.annotation.Excel;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 本体关系 Response VO
 */
@Schema(description = "本体关系 Response VO")
@Data
public class RelationRespVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "编号", example = "1")
    @Excel(name = "编号")
    private Long id;

    @Schema(description = "所属本体ID", example = "1")
    @Excel(name = "所属本体ID")
    private Long ontologyId;

    @Schema(description = "关系名称", example = "拥有订单")
    @Excel(name = "关系名称")
    private String name;

    @Schema(description = "关系编码", example = "has_order")
    @Excel(name = "关系编码")
    private String code;

    @Schema(description = "源概念ID", example = "1")
    @Excel(name = "源概念ID")
    private Long sourceConceptId;

    @Schema(description = "目标概念ID", example = "2")
    @Excel(name = "目标概念ID")
    private Long targetConceptId;

    @Schema(description = "关系类型：one_to_one/one_to_many/many_to_one/many_to_many", example = "one_to_many")
    @Excel(name = "关系类型")
    private String relationType;

    @Schema(description = "描述", example = "客户与订单的一对多关系")
    @Excel(name = "描述")
    private String description;

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
