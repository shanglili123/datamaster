package com.datamaster.module.assets.controller.admin.assetchild.audit.vo;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import com.datamaster.common.annotation.Excel;
import java.io.Serializable;
import java.util.Date;/** *  Response VO  DA_ASSET_AUDIT_SCHEDULE * * @author DATAMASTER * @date 2025-05-09 */
@Schema(description = " Response VO")
@Data
public class AssetsAssetAuditScheduleRespVO implements Serializable {
    private static final long serialVersionUID = 1L;
@Excel(name = "ID")    @Schema(description = "ID")    private Long id;
@Excel(name = "ID")    @Schema(description = "ID", example = "")    private Long assetId;
@Excel(name = "", readConverterExp = "0=1")    @Schema(description = "", example = "")    private String scheduleFlag;
@Excel(name = "cron")    @Schema(description = "cron", example = "")    private String cronExpression;
@Excel(name = "id")    @Schema(description = "id", example = "")    private Long nodeId;
@Excel(name = "")    @Schema(description = "", example = "")    private String nodeCode;
@Excel(name = "id")    @Schema(description = "id", example = "")    private Long taskId;
@Excel(name = "")    @Schema(description = "", example = "")    private String taskCode;
@Excel(name = "id")    @Schema(description = "id", example = "")    private Long systemJobId;
@Excel(name = "")    @Schema(description = "", example = "")    private Boolean validFlag;
@Excel(name = "")    @Schema(description = "", example = "")    private Boolean delFlag;
@Excel(name = "")    @Schema(description = "", example = "")    private String createBy;
@Excel(name = "id")    @Schema(description = "id", example = "")    private Long creatorId;
@Excel(name = "", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")    @Schema(description = "", example = "")    private Date createTime;
@Excel(name = "")    @Schema(description = "", example = "")    private String updateBy;
@Excel(name = "id")    @Schema(description = "id", example = "")    private Long updaterId;
@Excel(name = "", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")    @Schema(description = "", example = "")    private Date updateTime;
@Excel(name = "")    @Schema(description = "", example = "")    private String remark;}
