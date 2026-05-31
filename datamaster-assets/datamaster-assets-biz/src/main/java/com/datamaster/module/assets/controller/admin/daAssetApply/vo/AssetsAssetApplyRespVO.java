package com.datamaster.module.assets.controller.admin.daAssetApply.vo;
import com.baomidou.mybatisplus.annotation.TableField;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import com.datamaster.common.annotation.Excel;
import com.datamaster.module.assets.controller.admin.assetchild.theme.vo.AssetsAssetThemeRelRespVO;
import java.io.Serializable;
import java.util.Date;
import java.util.List;/** *  Response VO  DA_ASSET_APPLY * * @author shu * @date 2025-03-19 */
@Schema(description = " Response VO")
@Data
public class AssetsAssetApplyRespVO implements Serializable {
    private static final long serialVersionUID = 1L;
@Excel(name = "ID")    @Schema(description = "ID")    private Long id;
@Excel(name = "id")    @Schema(description = "id", example = "")    private Long assetId;
@Excel(name = "")    @Schema(description = "", example = "")    private String assetName;
@Excel(name = "")    @Schema(description = "", example = "")    private String assetTableName;
@Excel(name = "")    @Schema(description = "", example = "")    private String catAssetName;
@Excel(name = "")    @Schema(description = "", example = "")    private String catAssetCode;
@Excel(name = "01")    @Schema(description = "01", example = "")    private String sourceType;
@Excel(name = "id")    @Schema(description = "id", example = "")    private Long projectId;
@Excel(name = "")    @Schema(description = "", example = "")    private String projectName;
@Excel(name = "")    @Schema(description = "", example = "")    private String themeName;
@TableField(exist = false)    private List<AssetsAssetThemeRelRespVO> AssetsAssetThemeRelList;
@Excel(name = "")    @Schema(description = "", example = "")    private String projectCode;
@Excel(name = "")    @Schema(description = "", example = "")    private String applyReason;
@Excel(name = "")    @Schema(description = "", example = "")    private String approvalReason;
@Excel(name = "")    @Schema(description = "", example = "")    private String status;
@Excel(name = "")    @Schema(description = "", example = "")    private Boolean validFlag;
@Excel(name = "")    @Schema(description = "", example = "")    private Boolean delFlag;
@Excel(name = "")    @Schema(description = "", example = "")    private String createBy;
@Excel(name = "id")    @Schema(description = "id", example = "")    private Long creatorId;
@Excel(name = "", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")    @Schema(description = "", example = "")    private Date createTime;
@Excel(name = "")    @Schema(description = "", example = "")    private String updateBy;
@Excel(name = "id")    @Schema(description = "id", example = "")    private Long updaterId;
@Excel(name = "", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")    @Schema(description = "", example = "")    private Date updateTime;
@Excel(name = "")    @Schema(description = "", example = "")    private String remark;
@Excel(name = "")    @Schema(description = "", example = "")    private String datasourceName;
@Excel(name = "ip")    @Schema(description = "ip", example = "")    private String datasourceIp;
@Excel(name = "")    @Schema(description = "", example = "")    private String datasourceType;
@Excel(name = "")    @Schema(description = "", example = "")    private String description;
@Excel(name = "")    @Schema(description = "", example = "")    private String phonenumber;}
