package com.datamaster.module.assets.controller.admin.assetColumnProjectRel.vo;

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
public class AssetsAssetColumnProjectRelRespVO implements Serializable {
    private static final long serialVersionUID = 1L;

    @Excel(name = "ID")
    @Schema(description = "ID")
    private Long id;

    @Excel(name = "资产id")
    @Schema(description = "资产id", example = "")
    private Long assetId;

    @Excel(name = "字段id")
    @Schema(description = "字段id", example = "")
    private Long columnId;

    @Excel(name = "空间id")
    @Schema(description = "空间id", example = "")
    private Long projectId;

    @Excel(name = "空间编码")
    @Schema(description = "空间编码", example = "")
    private String projectCode;

    @Excel(name = "有效标识")
    @Schema(description = "有效标识", example = "")
    private Boolean validFlag;

    @Excel(name = "删除标识")
    @Schema(description = "删除标识", example = "")
    private Boolean delFlag;

    @Excel(name = "创建人")
    @Schema(description = "创建人", example = "")
    private String createBy;

    @Excel(name = "创建人id")
    @Schema(description = "创建人id", example = "")
    private Long creatorId;

    @Excel(name = "创建时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间", example = "")
    private Date createTime;

    @Excel(name = "更新人")
    @Schema(description = "更新人", example = "")
    private String updateBy;

    @Excel(name = "更新人id")
    @Schema(description = "更新人id", example = "")
    private Long updaterId;

    @Excel(name = "更新时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "更新时间", example = "")
    private Date updateTime;

    @Excel(name = "备注")
    @Schema(description = "备注", example = "")
    private String remark;
}
