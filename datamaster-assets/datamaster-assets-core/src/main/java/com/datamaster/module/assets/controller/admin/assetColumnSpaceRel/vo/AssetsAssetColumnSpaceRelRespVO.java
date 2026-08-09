package com.datamaster.module.assets.controller.admin.assetColumnSpaceRel.vo;

import com.datamaster.common.annotation.Excel;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 数据资产字段与空间关联关系 Response VO
 *
 * @author DATAMASTER
 */
@Schema(description = "数据资产字段与空间关联关系 Response VO")
@Data
public class AssetsAssetColumnSpaceRelRespVO implements Serializable {
    private static final long serialVersionUID = 1L;

    @Excel(name = "ID")
    @Schema(description = "ID")
    private Long id;

    @Excel(name = "资产 ID")
    @Schema(description = "资产 ID", example = "")
    private Long assetId;

    @Excel(name = "字段 ID")
    @Schema(description = "字段 ID", example = "")
    private Long columnId;

    @Excel(name = "空间 ID")
    @Schema(description = "空间 ID", example = "")
    private Long spaceId;

    @Excel(name = "空间编码")
    @Schema(description = "空间编码", example = "")
    private String spaceCode;

    @Excel(name = "有效标识")
    @Schema(description = "有效标识", example = "")
    private Boolean validFlag;

    @Excel(name = "删除标识")
    @Schema(description = "删除标识", example = "")
    private Boolean delFlag;

    @Excel(name = "创建人")
    @Schema(description = "创建人", example = "")
    private String createBy;

    @Excel(name = "创建人 ID")
    @Schema(description = "创建人 ID", example = "")
    private Long creatorId;

    @Excel(name = "创建时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间", example = "")
    private Date createTime;

    @Excel(name = "更新人")
    @Schema(description = "更新人", example = "")
    private String updateBy;

    @Excel(name = "更新人 ID")
    @Schema(description = "更新人 ID", example = "")
    private Long updaterId;

    @Excel(name = "更新时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "更新时间", example = "")
    private Date updateTime;

    @Excel(name = "描述")
    @Schema(description = "描述", example = "")
    private String description;
}
