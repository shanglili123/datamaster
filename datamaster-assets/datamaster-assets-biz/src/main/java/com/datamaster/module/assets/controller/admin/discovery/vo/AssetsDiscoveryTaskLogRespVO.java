package com.datamaster.module.assets.controller.admin.discovery.vo;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import com.datamaster.common.annotation.Excel;
import java.io.Serializable;
import java.util.Date;/** *  Response VO  DA_DISCOVERY_TASK_LOG * * @author DATAMASTER * @date 2025-02-17 */
@Schema(description = " Response VO")
@Data
public class AssetsDiscoveryTaskLogRespVO implements Serializable {
    private static final long serialVersionUID = 1L;
@Excel(name = "ID")    @Schema(description = "ID")    private Long id;
@Excel(name = "")    @Schema(description = "", example = "")    private String name;
@Excel(name = "id")    @Schema(description = "id", example = "")    private Long nodeId;
@Excel(name = "")    @Schema(description = "", example = "")    private String nodeCode;
@Excel(name = "")    @Schema(description = "", example = "")    private String taskName;
@Excel(name = "id")    @Schema(description = "id", example = "")    private Long taskId;
@Excel(name = "")    @Schema(description = "", example = "")    private String taskCode;
@Excel(name = "", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")    @Schema(description = "", example = "")    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")    private Date startTime;
@Excel(name = "", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")    @Schema(description = "", example = "")    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")    private Date endTime;
@Excel(name = "")    @Schema(description = "", example = "")    private String status;
@Excel(name = "")    @Schema(description = "", example = "")    private Long newTableCount;
@Excel(name = "")    @Schema(description = "", example = "")    private Long modifiedTableCount;
@Excel(name = "")    @Schema(description = "", example = "")    private Long deletedTableCount;
@Excel(name = "")    @Schema(description = "", example = "")    private String contact;
@Excel(name = "ID")    @Schema(description = "ID", example = "")    private Long contactId;
@Excel(name = "")    @Schema(description = "", example = "")    private String contactNumber;
@Excel(name = "")    @Schema(description = "", example = "")    private String email;
@Excel(name = "DolphinSchedulerid")    @Schema(description = "DolphinSchedulerid", example = "")    private Long dsId;
@Excel(name = "DolphinSchedulerid")    @Schema(description = "DolphinSchedulerid", example = "")    private Long dsTaskInstanceId;
@Excel(name = "")    @Schema(description = "", example = "")    private String path;
@Excel(name = "")    @Schema(description = "", example = "")    private Boolean validFlag;
@Excel(name = "")    @Schema(description = "", example = "")    private Boolean delFlag;
@Excel(name = "")    @Schema(description = "", example = "")    private String createBy;
@Excel(name = "id")    @Schema(description = "id", example = "")    private Long creatorId;
@Excel(name = "", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")    @Schema(description = "", example = "")    private Date createTime;
@Excel(name = "")    @Schema(description = "", example = "")    private String updateBy;
@Excel(name = "id")    @Schema(description = "id", example = "")    private Long updaterId;
@Excel(name = "", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")    @Schema(description = "", example = "")    private Date updateTime;
@Excel(name = "")    @Schema(description = "", example = "")    private String remark;}
