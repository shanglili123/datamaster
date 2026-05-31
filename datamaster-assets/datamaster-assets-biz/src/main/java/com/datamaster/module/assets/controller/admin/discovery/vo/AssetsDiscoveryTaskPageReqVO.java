package com.datamaster.module.assets.controller.admin.discovery.vo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import com.datamaster.common.core.page.PageParam;
import java.util.Date;/** *  Request VO  DA_DISCOVERY_TASK * * @author DATAMASTER * @date 2025-02-11 */
@Schema(description = " Request VO")
@Data
public class AssetsDiscoveryTaskPageReqVO extends PageParam {
    private static final long serialVersionUID = 1L;
@Schema(description = "ID", example = "")        private Long id;
@Schema(description = "", example = "")    private String name;
@Schema(description = "id", example = "")    private Long datasourceId;
@Schema(description = "", example = "")    private String status;
@Schema(description = "cron", example = "")    private String cronExpression;
@Schema(description = "", example = "")    private String contact;
@Schema(description = "", example = "")    private String email;
@Schema(description = "ID", example = "")    private Long contactId;    /**  */
@Schema(description = "", example = "")    private Long lastTableCount;
@Schema(description = "", example = "")    private String contactNumber;
@Schema(description = "", example = "")    private String catCode;
@Schema(description = "", example = "")    private String description;
@Schema(description = "id", example = "")    private Long systemJobId;
@Schema(description = "", example = "")    private Date lastExecuteTime;    /** id */
@Schema(description = "id", example = "")    private Long nodeId;    /**  */
@Schema(description = "", example = "")    private String nodeCode;    /** id */
@Schema(description = "id", example = "")    private Long taskId;    /**  */
@Schema(description = "", example = "")    private String taskCode;}
