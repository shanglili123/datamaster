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
@Schema(description = "任务名称", example = "")    private String name;
@Schema(description = "数据源 ID", example = "")    private Long datasourceId;
@Schema(description = "任务状态", example = "")    private String status;
@Schema(description = "cron", example = "")    private String cronExpression;
@Schema(description = "联系人", example = "")    private String contact;
@Schema(description = "邮箱", example = "")    private String email;
@Schema(description = "ID", example = "")    private Long contactId;    /**  */
@Schema(description = "最近采集表数量", example = "")    private Long lastTableCount;
@Schema(description = "联系电话", example = "")    private String contactNumber;
@Schema(description = "目录编码", example = "")    private String catCode;
@Schema(description = "描述", example = "")    private String description;
@Schema(description = "系统任务 ID", example = "")    private Long systemJobId;
@Schema(description = "最近执行时间", example = "")    private Date lastExecuteTime;    /** id */
@Schema(description = "节点 ID", example = "")    private Long nodeId;    /**  */
@Schema(description = "节点编码", example = "")    private String nodeCode;    /** id */
@Schema(description = "任务 ID", example = "")    private Long taskId;    /**  */
@Schema(description = "任务编码", example = "")    private String taskCode;}
