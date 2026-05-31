package com.datamaster.module.assets.controller.admin.discovery.vo;
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
@Schema(description = "", example = "")    @Size(max = 256, message = "256")    private String name;
@Schema(description = "id", example = "")    private Long datasourceId;
@Schema(description = "", example = "")    @Size(max = 256, message = "256")    private String status;
@Schema(description = "cron", example = "")    @Size(max = 256, message = "cron256")    private String cronExpression;
@Schema(description = "", example = "")    @Size(max = 256, message = "256")    private String contact;
@Schema(description = "", example = "")    @Size(max = 256, message = "256")    private String email;
@Schema(description = "ID", example = "")    private Long contactId;    /**  */
@Schema(description = "")    private Long lastTableCount;
@Schema(description = "", example = "")    @Size(max = 256, message = "256")    private String contactNumber;
@Schema(description = "", example = "")    @Size(max = 256, message = "256")    private String catCode;
@Schema(description = "", example = "")    @Size(max = 256, message = "256")    private String description;
@Schema(description = "id", example = "")    private Long systemJobId;
@Schema(description = "", example = "")    private Date lastExecuteTime;    /** id */
@Schema(description = "id", example = "")    private Long nodeId;    /**  */
@Schema(description = "", example = "")    private String nodeCode;    /** id */
@Schema(description = "id", example = "")    private Long taskId;    /**  */
@Schema(description = "", example = "")    private String taskCode;
@Schema(description = "", example = "")    @Size(max = 256, message = "256")    private String remark;
@TableField(exist = false)    private String misfirePolicy;
@TableField(exist = false)    private String jobGroup;
@TableField(exist = false)    private String concurrent;}
