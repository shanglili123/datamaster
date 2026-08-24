package com.datamaster.module.ontology.controller.admin.ontology.vo;

import com.datamaster.common.annotation.Excel;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 本体 Response VO
 */
@Schema(description = "本体 Response VO")
@Data
public class OntologyRespVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "编号", example = "1")
    @Excel(name = "编号")
    private Long id;

    @Schema(description = "本体名称", example = "客户本体")
    @Excel(name = "本体名称")
    private String name;

    @Schema(description = "本体编码", example = "customer")
    @Excel(name = "本体编码")
    private String code;

    @Schema(description = "描述", example = "客户域本体模型")
    @Excel(name = "描述")
    private String description;

    @Schema(description = "所属空间ID", example = "1")
    @Excel(name = "所属空间ID")
    private Long spaceId;

    @Schema(description = "空间编码", example = "space01")
    @Excel(name = "空间编码")
    private String spaceCode;

    @Schema(description = "状态：0=草稿 1=已发布 2=已归档", example = "0")
    @Excel(name = "状态", readConverterExp = "0=草稿,1=已发布,2=已归档")
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
