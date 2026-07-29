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
@Excel(name = "任务名称")    @Schema(description = "任务名称", example = "")    private String name;
@Excel(name = "节点 ID")    @Schema(description = "节点 ID", example = "")    private Long nodeId;
@Excel(name = "节点编码")    @Schema(description = "节点编码", example = "")    private String nodeCode;
@Excel(name = "")    @Schema(description = "", example = "")    private String taskName;
@Excel(name = "任务 ID")    @Schema(description = "任务 ID", example = "")    private Long taskId;
@Excel(name = "任务编码")    @Schema(description = "任务编码", example = "")    private String taskCode;
@Excel(name = "", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")    @Schema(description = "", example = "")    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")    private Date startTime;
@Excel(name = "", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")    @Schema(description = "", example = "")    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")    private Date endTime;
@Excel(name = "任务状态")    @Schema(description = "任务状态", example = "")    private String status;
@Excel(name = "")    @Schema(description = "", example = "")    private Long newTableCount;
@Excel(name = "")    @Schema(description = "", example = "")    private Long modifiedTableCount;
@Excel(name = "")    @Schema(description = "", example = "")    private Long deletedTableCount;
@Excel(name = "联系人")    @Schema(description = "联系人", example = "")    private String contact;
@Excel(name = "ID")    @Schema(description = "ID", example = "")    private Long contactId;
@Excel(name = "联系电话")    @Schema(description = "联系电话", example = "")    private String contactNumber;
@Excel(name = "邮箱")    @Schema(description = "邮箱", example = "")    private String email;
@Excel(name = "DolphinSchedulerid")    @Schema(description = "DolphinSchedulerid", example = "")    private Long dsId;
@Excel(name = "DolphinSchedulerid")    @Schema(description = "DolphinSchedulerid", example = "")    private Long dsTaskInstanceId;
@Excel(name = "")    @Schema(description = "", example = "")    private String path;
@Excel(name = "是否有效")    @Schema(description = "是否有效", example = "")    private Boolean validFlag;
@Excel(name = "删除标志")    @Schema(description = "删除标志", example = "")    private Boolean delFlag;
@Excel(name = "创建人")    @Schema(description = "创建人", example = "")    private String createBy;
@Excel(name = "创建人 ID")    @Schema(description = "创建人 ID", example = "")    private Long creatorId;
@Excel(name = "创建时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")    @Schema(description = "创建时间", example = "")    private Date createTime;
@Excel(name = "更新人")    @Schema(description = "更新人", example = "")    private String updateBy;
@Excel(name = "更新人 ID")    @Schema(description = "更新人 ID", example = "")    private Long updaterId;
@Excel(name = "更新时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")    @Schema(description = "更新时间", example = "")    private Date updateTime;
@Excel(name = "描述")    @Schema(description = "描述", example = "")    private String description;}
