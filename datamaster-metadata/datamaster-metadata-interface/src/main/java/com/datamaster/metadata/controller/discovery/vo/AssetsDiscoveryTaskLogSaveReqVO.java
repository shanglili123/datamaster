package com.datamaster.metadata.controller.discovery.vo;

import com.fasterxml.jackson.annotation.JsonFormat;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Data;

import com.datamaster.common.core.domain.BaseEntity;

import javax.validation.constraints.Size;

import java.util.Date;
/** *  / Request VO DA_DISCOVERY_TASK_LOG * * @author lili.shang * @date 2025-02-17 */

@Schema(description = " Response VO")
@Data
public class AssetsDiscoveryTaskLogSaveReqVO extends BaseEntity {
    private static final long serialVersionUID = 1L;

@Schema(description = "ID")    private Long id;

@Schema(description = "", example = "")
@Size(max = 256, message = "任务名称长度不能超过256个字符")    private String name;

@Schema(description = "节点 ID", example = "")    private Long nodeId;

@Schema(description = "", example = "")
@Size(max = 256, message = "节点编码长度不能超过256个字符")    private String nodeCode;

@Schema(description = "", example = "")
@Size(max = 256, message = "256")    private String taskName;

@Schema(description = "任务 ID", example = "")    private Long taskId;

@Schema(description = "", example = "")
@Size(max = 256, message = "任务编码长度不能超过256个字符")    private String taskCode;

@Schema(description = "", example = "")
@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")    private Date startTime;

@Schema(description = "", example = "")
@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")    private Date endTime;

@Schema(description = "", example = "")
@Size(max = 256, message = "任务状态长度不能超过256个字符")    private String status;

@Schema(description = "", example = "")    private Long newTableCount;

@Schema(description = "", example = "")    private Long modifiedTableCount;

@Schema(description = "", example = "")    private Long deletedTableCount;

@Schema(description = "", example = "")
@Size(max = 256, message = "联系人长度不能超过256个字符")    private String contact;

@Schema(description = "ID", example = "")    private Long contactId;

@Schema(description = "", example = "")
@Size(max = 256, message = "联系电话长度不能超过256个字符")    private String contactNumber;

@Schema(description = "", example = "")
@Size(max = 256, message = "邮箱长度不能超过256个字符")    private String email;

@Schema(description = "DolphinSchedulerid", example = "")    private Long dsId;

@Schema(description = "DolphinSchedulerid", example = "")    private Long dsTaskInstanceId;

@Schema(description = "", example = "")
@Size(max = 256, message = "256")    private String path;

@Schema(description = "", example = "")
@Size(max = 256, message = "描述长度不能超过256个字符")    private String description;
    public void populateFromTask(AssetsDiscoveryTaskRespVO AssetsDiscoveryTask) {
        if (AssetsDiscoveryTask == null) {
        return;
        }
//TODO 对接海豚调度器会改        this.name = AssetsDiscoveryTask.getName();        this.taskName = AssetsDiscoveryTask.getName();        this.description = AssetsDiscoveryTask.getDescription();        this.contact = AssetsDiscoveryTask.getContact();
    this.contactId = AssetsDiscoveryTask.getContactId();
    this.contactNumber = AssetsDiscoveryTask.getContactNumber();
    this.email = AssetsDiscoveryTask.getEmail();
    this.nodeId = AssetsDiscoveryTask.getNodeId();
    this.nodeCode = AssetsDiscoveryTask.getNodeCode();
    this.taskId = AssetsDiscoveryTask.getTaskId();
    this.taskCode = AssetsDiscoveryTask.getTaskCode();
    this.dsId = 0L;
    this.dsTaskInstanceId = 0L;
    this.status = "1";
    }}
