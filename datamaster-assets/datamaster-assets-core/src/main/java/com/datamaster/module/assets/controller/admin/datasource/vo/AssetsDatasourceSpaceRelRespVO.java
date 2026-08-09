package com.datamaster.module.assets.controller.admin.datasource.vo;

import com.datamaster.common.annotation.Excel;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 数据源与空间关系 Response VO
 *
 * @author DATAMASTER
 * @date 2025-03-13
 */
@Schema(description = "数据源与空间关系 Response VO")
@Data
public class AssetsDatasourceSpaceRelRespVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Excel(name = "ID")
    @Schema(description = "ID")
    private Long id;

    @Excel(name = "空间ID")
    @Schema(description = "空间ID", example = "1")
    private Long spaceId;

    @Excel(name = "空间编码")
    @Schema(description = "空间编码", example = "bank_risk")
    private String spaceCode;

    @Excel(name = "是否已分配到数据加工")
    @Schema(description = "是否已分配到数据加工", example = "false")
    private Boolean dppAssigned;

    @Excel(name = "数据源ID")
    @Schema(description = "数据源ID", example = "1")
    private Long datasourceId;

    @Excel(name = "描述")
    @Schema(description = "描述", example = "空间可使用该数据源")
    private String description;

    @Excel(name = "是否有效")
    @Schema(description = "是否有效", example = "true")
    private Boolean validFlag;

    @Excel(name = "创建人")
    @Schema(description = "创建人", example = "admin")
    private String createBy;

    @Excel(name = "创建人ID")
    @Schema(description = "创建人ID", example = "1")
    private Long creatorId;

    @Excel(name = "创建时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间", example = "2026-07-28 10:00:00")
    private Date createTime;

    @Excel(name = "更新人")
    @Schema(description = "更新人", example = "admin")
    private String updateBy;

    @Excel(name = "更新人ID")
    @Schema(description = "更新人ID", example = "1")
    private Long updaterId;

    @Excel(name = "更新时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "更新时间", example = "2026-07-28 10:00:00")
    private Date updateTime;

}
