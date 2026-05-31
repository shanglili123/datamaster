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
@Schema(description = "", example = "")    private String name;
@Schema(description = "id", example = "")    private Long nodeId;
@Schema(description = "", example = "")    private String nodeCode;
@Schema(description = "", example = "")    private String taskName;
@Schema(description = "id", example = "")    private Long taskId;
@Schema(description = "", example = "")    private String taskCode;
@Schema(description = "", example = "")    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")    private Date startTime;
@Schema(description = "", example = "")    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")    private Date endTime;
@Schema(description = "", example = "")    private String status;
@Schema(description = "", example = "")    private Long newTableCount;
@Schema(description = "", example = "")    private Long modifiedTableCount;
@Schema(description = "", example = "")    private Long deletedTableCount;
@Schema(description = "", example = "")    private String contact;
@Schema(description = "ID", example = "")    private Long contactId;
@Schema(description = "", example = "")    private String contactNumber;
@Schema(description = "", example = "")    private String email;
@Schema(description = "DolphinSchedulerid", example = "")    private Long dsId;
@Schema(description = "DolphinSchedulerid", example = "")    private Long dsTaskInstanceId;
@Schema(description = "", example = "")    private String path;}
