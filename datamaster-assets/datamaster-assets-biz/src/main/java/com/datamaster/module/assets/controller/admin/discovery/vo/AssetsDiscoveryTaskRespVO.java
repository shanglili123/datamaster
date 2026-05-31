package com.datamaster.module.assets.controller.admin.discovery.vo;
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
@Excel(name = "")    @Schema(description = "", example = "")    private String name;
@Excel(name = "id")    @Schema(description = "id", example = "")    private Long datasourceId;
@Excel(name = " 0: 1:")    @Schema(description = "", example = "")    private String status;
@Excel(name = "cron")    @Schema(description = "cron", example = "")    private String cronExpression;
@Excel(name = "")    @Schema(description = "", example = "")    private String contact;
@Schema(description = "", example = "")    private String email;
@Excel(name = "ID")    @Schema(description = "ID", example = "")    private Long contactId;    /**  */
@Excel(name = "")    @Schema(description = "", example = "")    private Long lastTableCount;
@Excel(name = "")    @Schema(description = "", example = "")    private String contactNumber;
@Excel(name = "")    @Schema(description = "", example = "")    private String catCode;
@TableField(exist = false)    private String catName;
@Excel(name = "")    @Schema(description = "", example = "")    @TableField(exist = false)    private String datasourceType;
@Excel(name = "")    @Schema(description = "", example = "")    @TableField(exist = false)    private String datasourceName;
@Excel(name = "")    @Schema(description = "", example = "")    private String description;
@Excel(name = "id")    @Schema(description = "id", example = "")    private Long systemJobId;
@Excel(name = "", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")    @Schema(description = "", example = "")    private Date lastExecuteTime;    /** id */
@Schema(description = "id", example = "")    private Long nodeId;    /**  */
@Schema(description = "", example = "")    private String nodeCode;    /** id */
@Schema(description = "id", example = "")    private Long taskId;    /**  */
@Schema(description = "", example = "")    private String taskCode;
@Excel(name = "")    @Schema(description = "", example = "")    private Boolean validFlag;
@Excel(name = "")    @Schema(description = "", example = "")    private Boolean delFlag;
@Excel(name = "")    @Schema(description = "", example = "")    private String createBy;
@Excel(name = "id")    @Schema(description = "id", example = "")    private Long creatorId;
@Excel(name = "", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")    @Schema(description = "", example = "")    private Date createTime;
@Excel(name = "")    @Schema(description = "", example = "")    private String updateBy;
@Excel(name = "id")    @Schema(description = "id", example = "")    private Long updaterId;
@Excel(name = "", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")    @Schema(description = "", example = "")    private Date updateTime;
@Excel(name = "")    @Schema(description = "", example = "")    private String remark;    /**     *      */
@TableField(exist = false)    private long countPending;    /**     *      */
@TableField(exist = false)    private long countSubmitted;    /**     *      */
@TableField(exist = false)    private long countIgnoreFlag;
@TableField(exist = false)    private String ip;
@TableField(exist = false)    private String misfirePolicy;
@TableField(exist = false)    private String jobGroup;
@TableField(exist = false)    private String concurrent;}
