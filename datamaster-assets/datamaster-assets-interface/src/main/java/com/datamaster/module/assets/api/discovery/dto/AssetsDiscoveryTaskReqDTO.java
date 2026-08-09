
package com.datamaster.module.assets.api.discovery.dto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.util.Date;/** * 数据发现任务 DTO 对象 DA_DISCOVERY_TASK * * @author DATAMASTER * @date 2025-02-11 */
@Data
public class AssetsDiscoveryTaskReqDTO {
    private static final long serialVersionUID = 1L;    /** ID */    private Long id;    /** 任务名称 */    private String name;    /** 数据连接id */    private Long datasourceId;    /** 任务状态 */    private String status;    /** cron执行表达式 */    private String cronExpression;    /** 联系人 */    private String contact;
@Schema(description = "邮箱", example = "")    private String email;    /** 联系人ID */    private Long contactId;    /** 上次变化表数 */    private Long lastTableCount;    /** 联系电话 */    private String contactNumber;    /** 类目编码 */    private String catCode;    /** 描述 */    private String description;    /** 定时任务调度表id */    private Long systemJobId;    /** 最后执行时间 */    private Date lastExecuteTime;    /** 节点id */
@Schema(description = "节点id", example = "")    private Long nodeId;    /** 节点编码 */
@Schema(description = "节点编码", example = "")    private String nodeCode;    /** 任务id */
@Schema(description = "任务id", example = "")    private Long taskId;    /** 任务编码 */
@Schema(description = "任务编码", example = "")    private String taskCode;    /** 是否有效 */    private Boolean validFlag;    /** 删除标志 */    private Boolean delFlag;}
