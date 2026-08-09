package com.datamaster.metadata.controller.discovery.vo;
import com.baomidou.mybatisplus.annotation.TableField;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import com.datamaster.common.core.domain.BaseEntity;
import javax.validation.constraints.Size;
import java.util.Date;/** *  / Request VO DA_DISCOVERY_TASK * * @author DATAMASTER * @date 2025-02-11 */
@Schema(description = " Response VO")
@Data
public class AssetsDiscoveryTaskSaveReqVO extends BaseEntity {
    private static final long serialVersionUID = 1L;
@Schema(description = "ID")    private Long id;
@Schema(description = "任务名称", example = "")    @Size(max = 256, message = "任务名称长度不能超过256个字符")    private String name;
@Schema(description = "数据源 ID", example = "")    private Long datasourceId;
@Schema(description = "任务状态", example = "")    @Size(max = 256, message = "任务状态长度不能超过256个字符")    private String status;
@Schema(description = "cron", example = "")    @Size(max = 256, message = "Cron 表达式长度不能超过256个字符")    private String cronExpression;
@Schema(description = "联系人", example = "")    @Size(max = 256, message = "联系人长度不能超过256个字符")    private String contact;
@Schema(description = "邮箱", example = "")    @Size(max = 256, message = "邮箱长度不能超过256个字符")    private String email;
@Schema(description = "ID", example = "")    private Long contactId;    /**  */
@Schema(description = "最近采集表数量")    private Long lastTableCount;
@Schema(description = "联系电话", example = "")    @Size(max = 256, message = "联系电话长度不能超过256个字符")    private String contactNumber;
@Schema(description = "目录编码", example = "")    @Size(max = 256, message = "目录编码长度不能超过256个字符")    private String catCode;
@Schema(description = "描述", example = "")    @Size(max = 256, message = "描述长度不能超过256个字符")    private String description;
@Schema(description = "系统任务 ID", example = "")    private Long systemJobId;
@Schema(description = "最近执行时间", example = "")    private Date lastExecuteTime;    /** id */
@Schema(description = "节点 ID", example = "")    private Long nodeId;    /**  */
@Schema(description = "节点编码", example = "")    private String nodeCode;    /** id */
@Schema(description = "任务 ID", example = "")    private Long taskId;    /**  */
@Schema(description = "任务编码", example = "")    private String taskCode;
@Schema(description = "", example = "")    @Size(max = 256, message = "256")
@TableField(exist = false)    private String misfirePolicy;
@TableField(exist = false)    private String jobGroup;
@TableField(exist = false)    private String concurrent;}
