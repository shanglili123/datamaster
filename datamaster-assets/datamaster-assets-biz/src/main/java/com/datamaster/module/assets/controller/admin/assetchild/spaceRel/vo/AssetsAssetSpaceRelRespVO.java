package com.datamaster.module.assets.controller.admin.assetchild.spaceRel.vo;

import com.datamaster.common.annotation.Excel;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 数据资产与空间关联关系 Response VO
 *
 * @author DATAMASTER
 * @date 2025-04-18
 */
@Schema(description = "数据资产与空间关联关系 Response VO")
@Data
public class AssetsAssetSpaceRelRespVO implements Serializable {
    private static final long serialVersionUID = 1L;

    @Excel(name = "ID")
    @Schema(description = "ID")
    private Long id;

    @Excel(name = "资产 ID")
    @Schema(description = "资产 ID", example = "1")
    private Long assetId;

    @Excel(name = "空间 ID")
    @Schema(description = "空间 ID", example = "1")
    private Long spaceId;

    @Excel(name = "空间编码")
    @Schema(description = "空间编码", example = "bank_risk")
    private String spaceCode;

    @Excel(name = "是否有效")
    @Schema(description = "是否有效", example = "true")
    private Boolean validFlag;

    @Excel(name = "删除标志")
    @Schema(description = "删除标志", example = "false")
    private Boolean delFlag;

    @Excel(name = "创建人")
    @Schema(description = "创建人", example = "admin")
    private String createBy;

    @Excel(name = "创建人 ID")
    @Schema(description = "创建人 ID", example = "1")
    private Long creatorId;

    @Excel(name = "创建时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间", example = "2026-07-28 10:00:00")
    private Date createTime;

    @Excel(name = "更新人")
    @Schema(description = "更新人", example = "admin")
    private String updateBy;

    @Excel(name = "更新人 ID")
    @Schema(description = "更新人 ID", example = "1")
    private Long updaterId;

    @Excel(name = "更新时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "更新时间", example = "2026-07-28 10:00:00")
    private Date updateTime;

    @Excel(name = "描述")
    @Schema(description = "描述", example = "资产可归属该空间")
    private String description;
}
