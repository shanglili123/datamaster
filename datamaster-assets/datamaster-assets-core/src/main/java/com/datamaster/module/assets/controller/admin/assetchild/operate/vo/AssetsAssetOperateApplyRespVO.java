package com.datamaster.module.assets.controller.admin.assetchild.operate.vo;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import com.datamaster.common.annotation.Excel;
import java.io.Serializable;
import java.util.Date;/** *  Response VO  DA_ASSET_OPERATE_APPLY * * @author DATAMASTER * @date 2025-05-09 */
@Schema(description = " Response VO")
@Data
public class AssetsAssetOperateApplyRespVO implements Serializable {
    private static final long serialVersionUID = 1L;
@Excel(name = "ID")    @Schema(description = "ID")    private Long id;
@Excel(name = "id")    @Schema(description = "id", example = "")    private Long assetId;
@Excel(name = "id")    @Schema(description = "id", example = "")    private Long datasourceId;
@Excel(name = "")    @Schema(description = "", example = "")    private String tableName;
@Excel(name = "/")    @Schema(description = "/", example = "")    private String tableComment;
@Excel(name = "")    @Schema(description = "", example = "")    private String operateType;
@Excel(name = "JSON")    @Schema(description = "JSON", example = "")    private String operateJson;
@Excel(name = "", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")    @Schema(description = "", example = "")    private Date operateTime;
@Excel(name = "")    @Schema(description = "", example = "")    private String executeFlag;
@Excel(name = "", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")    @Schema(description = "", example = "")    private Date executeTime;
@Excel(name = "审批执行状态")    @Schema(description = "审批执行状态")    private String approvalStatus;
@Excel(name = "审批意见")    @Schema(description = "审批意见")    private String approvalComment;
@Excel(name = "审批人")    @Schema(description = "审批人")    private String approvedBy;
@Excel(name = "审批人ID")    @Schema(description = "审批人ID")    private Long approvedId;
@Excel(name = "", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")    @Schema(description = "审批时间")    private Date approvedTime;
@Excel(name = "执行结果")    @Schema(description = "执行结果")    private String executionMessage;
@Excel(name = "")    @Schema(description = "", example = "")    private Boolean validFlag;
@Excel(name = "")    @Schema(description = "", example = "")    private Boolean delFlag;
@Excel(name = "")    @Schema(description = "", example = "")    private String createBy;
@Excel(name = "id")    @Schema(description = "id", example = "")    private Long creatorId;
@Excel(name = "", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")    @Schema(description = "", example = "")    private Date createTime;
@Excel(name = "")    @Schema(description = "", example = "")    private String updateBy;
@Excel(name = "id")    @Schema(description = "id", example = "")    private Long updaterId;
@Excel(name = "", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")    @Schema(description = "", example = "")    private Date updateTime;
@Excel(name = "")    @Schema(description = "", example = "")    private String description;}
