
package com.datamaster.module.assets.api.discovery.dto;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.util.Date;/** * 数据发现任务日志 DTO 对象 DA_DISCOVERY_TASK_LOG * * @author DATAMASTER * @date 2025-02-17 */
@Data
public class AssetsDiscoveryTaskLogRespDTO {
    private static final long serialVersionUID = 1L;    /** ID */    private Long id;    /** 实例名称 */    private String name;    /** 节点id */    private Long nodeId;    /** 节点编码 */    private String nodeCode;    /** 任务名称 */    private String taskName;    /** 任务id */    private Long taskId;    /** 任务编码 */    private String taskCode;    /** 开始时间 */
@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")    private Date startTime;    /** 结束时间 */
@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")    private Date endTime;    /** 状态 */    private String status;    /** 新增表数 */    private Long newTableCount;    /** 修改表数 */    private Long modifiedTableCount;    /** 删除表数 */    private Long deletedTableCount;    /** 联系人 */    private String contact;    /** 联系人ID */    private Long contactId;    /** 联系电话 */    private String contactNumber;    /** 邮箱 */    private String email;    /** DolphinScheduler的id */    private Long dsId;    /** DolphinScheduler的任务实例id */    private Long dsTaskInstanceId;    /** 日志路径 */    private String path;    /** 是否有效 */    private Boolean validFlag;    /** 删除标志 */    private Boolean delFlag;}
