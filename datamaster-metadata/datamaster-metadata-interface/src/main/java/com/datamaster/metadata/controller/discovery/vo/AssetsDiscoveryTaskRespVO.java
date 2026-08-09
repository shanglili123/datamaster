package com.datamaster.metadata.controller.discovery.vo;
import com.baomidou.mybatisplus.annotation.TableField;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import com.datamaster.common.annotation.Excel;
import java.io.Serializable;
import java.util.Date;/** *  Response VO  DA_DISCOVERY_TASK * * @author DATAMASTER * @date 2025-02-11 */
@Schema(description = " Response VO")
@Data
public class AssetsDiscoveryTaskRespVO implements Serializable {
    private static final long serialVersionUID = 1L;
@Excel(name = "ID")    @Schema(description = "ID")    private Long id;
@Excel(name = "任务名称")    @Schema(description = "任务名称", example = "")    private String name;
@Excel(name = "数据源 ID")    @Schema(description = "数据源 ID", example = "")    private Long datasourceId;
@Excel(name = " 0: 1:")    @Schema(description = "任务状态", example = "")    private String status;
@Excel(name = "cron")    @Schema(description = "cron", example = "")    private String cronExpression;
@Excel(name = "联系人")    @Schema(description = "联系人", example = "")    private String contact;
@Schema(description = "邮箱", example = "")    private String email;
@Excel(name = "ID")    @Schema(description = "ID", example = "")    private Long contactId;    /**  */
@Excel(name = "最近采集表数量")    @Schema(description = "最近采集表数量", example = "")    private Long lastTableCount;
@Excel(name = "联系电话")    @Schema(description = "联系电话", example = "")    private String contactNumber;
@Excel(name = "目录编码")    @Schema(description = "目录编码", example = "")    private String catCode;
@TableField(exist = false)    private String catName;
@Excel(name = "数据源类型")    @Schema(description = "数据源类型", example = "")    @TableField(exist = false)    private String datasourceType;
@Excel(name = "数据源名称")    @Schema(description = "数据源名称", example = "")    @TableField(exist = false)    private String datasourceName;
@Excel(name = "描述")    @Schema(description = "描述", example = "")    private String description;
@Excel(name = "系统任务 ID")    @Schema(description = "系统任务 ID", example = "")    private Long systemJobId;
@Excel(name = "最近执行时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")    @Schema(description = "最近执行时间", example = "")    private Date lastExecuteTime;    /** id */
@Schema(description = "节点 ID", example = "")    private Long nodeId;    /**  */
@Schema(description = "节点编码", example = "")    private String nodeCode;    /** id */
@Schema(description = "任务 ID", example = "")    private Long taskId;    /**  */
@Schema(description = "任务编码", example = "")    private String taskCode;
@Excel(name = "是否有效")    @Schema(description = "是否有效", example = "")    private Boolean validFlag;
@Excel(name = "删除标志")    @Schema(description = "删除标志", example = "")    private Boolean delFlag;
@Excel(name = "创建人")    @Schema(description = "创建人", example = "")    private String createBy;
@Excel(name = "创建人 ID")    @Schema(description = "创建人 ID", example = "")    private Long creatorId;
@Excel(name = "创建时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")    @Schema(description = "创建时间", example = "")    private Date createTime;
@Excel(name = "更新人")    @Schema(description = "更新人", example = "")    private String updateBy;
@Excel(name = "更新人 ID")    @Schema(description = "更新人 ID", example = "")    private Long updaterId;
@Excel(name = "更新时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")    @Schema(description = "更新时间", example = "")    private Date updateTime;
@Excel(name = "")    @Schema(description = "", example = "")    /**     *      */
@TableField(exist = false)    private long countPending;    /**     *      */
@TableField(exist = false)    private long countSubmitted;    /**     *      */
@TableField(exist = false)    private long countIgnoreFlag;
@TableField(exist = false)    private String ip;
@TableField(exist = false)    private String misfirePolicy;
@TableField(exist = false)    private String jobGroup;
@TableField(exist = false)    private String concurrent;}
