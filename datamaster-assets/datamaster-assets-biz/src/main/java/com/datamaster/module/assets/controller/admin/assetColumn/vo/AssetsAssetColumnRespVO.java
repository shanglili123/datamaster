package com.datamaster.module.assets.controller.admin.assetColumn.vo;
import com.baomidou.mybatisplus.annotation.TableField;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import com.datamaster.common.annotation.Excel;
import java.io.Serializable;
import java.util.Date;
import java.util.Set;/** *  Response VO  DA_ASSET_COLUMN * * @author lhs * @date 2025-01-21 */
@Schema(description = " Response VO")
@Data
public class AssetsAssetColumnRespVO implements Serializable {
    private static final long serialVersionUID = 1L;
@Excel(name = "ID")    @Schema(description = "ID")    private Long id;
@Excel(name = "id")    @Schema(description = "id", example = "")    private String assetId;
@Excel(name = "/")    @Schema(description = "/", example = "")    private String columnName;
@Excel(name = "/")    @Schema(description = "/", example = "")    private String columnComment;
@Excel(name = "")    @Schema(description = "", example = "")    private String columnType;
@Excel(name = "")    @Schema(description = "", example = "")    private Long columnLength;
@Excel(name = "")    @Schema(description = "", example = "")    private Long columnScale;
@Excel(name = "")    @Schema(description = "", example = "")    private String nullableFlag;
@Excel(name = "")    @Schema(description = "", example = "")    private String pkFlag;
@Excel(name = "")    @Schema(description = "", example = "")    private String defaultValue;
@Excel(name = "")    @Schema(description = "", example = "")    private String dataElemCodeFlag;
@Excel(name = "id")    @Schema(description = "id", example = "")    private String dataElemCodeId;
@Excel(name = "")    @Schema(description = "", example = "")    @TableField(exist = false)    private String dataElemCodeName;
@Excel(name = "id")    @Schema(description = "id", example = "")    private String sensitiveLevelId;
@Excel(name = "")    @Schema(description = "", example = "")    @TableField(exist = false)    private String sensitiveLevelName;
@Excel(name = "")    @Schema(description = "", example = "")    private String relDataElmeFlag;
@Excel(name = "")    @Schema(description = "", example = "")    @TableField(exist = false)    private String relDataElmeName;
@Excel(name = "")    @Schema(description = "", example = "")    private String relCleanFlag;
@Excel(name = "")    @Schema(description = "", example = "")    private String relAuditFlag;
@Excel(name = "")    @Schema(description = "", example = "")    private String description;
@Excel(name = "")    @Schema(description = "", example = "")    private Boolean validFlag;
@Excel(name = "")    @Schema(description = "", example = "")    private Boolean delFlag;
@Excel(name = "")    @Schema(description = "", example = "")    private String createBy;
@Excel(name = "id")    @Schema(description = "id", example = "")    private Long creatorId;
@Excel(name = "", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")    @Schema(description = "", example = "")    private Date createTime;
@Excel(name = "")    @Schema(description = "", example = "")    private String updateBy;
@Excel(name = "id")    @Schema(description = "id", example = "")    private Long updaterId;
@Excel(name = "", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")    @Schema(description = "", example = "")    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")    private Date updateTime;
@Excel(name = "")    @Schema(description = "", example = "")    private String remark;
@TableField(exist = false)    private Set<Long> elementId;}
