package com.datamaster.module.assets.controller.admin.discovery.vo;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import com.datamaster.common.core.page.PageParam;
import java.util.Date;/** *  Request VO  DA_DISCOVERY_TASK_LOG * * @author DATAMASTER * @date 2025-02-17 */
@Schema(description = " Request VO")
@Data
public class AssetsDiscoveryTaskLogPageReqVO extends PageParam {
    private static final long serialVersionUID = 1L;
@Schema(description = "ID", example = "")        private Long id;
@Schema(description = "任务名称", example = "")    private String name;
@Schema(description = "节点 ID", example = "")    private Long nodeId;
@Schema(description = "节点编码", example = "")    private String nodeCode;
@Schema(description = "", example = "")    private String taskName;
@Schema(description = "任务 ID", example = "")    private Long taskId;
@Schema(description = "任务编码", example = "")    private String taskCode;
@Schema(description = "", example = "")    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")    private Date startTime;
@Schema(description = "", example = "")    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")    private Date endTime;
@Schema(description = "任务状态", example = "")    private String status;
@Schema(description = "", example = "")    private Long newTableCount;
@Schema(description = "", example = "")    private Long modifiedTableCount;
@Schema(description = "", example = "")    private Long deletedTableCount;
@Schema(description = "联系人", example = "")    private String contact;
@Schema(description = "ID", example = "")    private Long contactId;
@Schema(description = "联系电话", example = "")    private String contactNumber;
@Schema(description = "邮箱", example = "")    private String email;
@Schema(description = "DolphinSchedulerid", example = "")    private Long dsId;
@Schema(description = "DolphinSchedulerid", example = "")    private Long dsTaskInstanceId;
@Schema(description = "", example = "")    private String path;}
