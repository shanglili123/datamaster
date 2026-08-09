package com.datamaster.metadata.dal.dataobject.discovery;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.SuperBuilder;
import com.datamaster.common.core.domain.BaseEntity;

import java.util.Date;

/**
 *  DO  AST_DISCOVERY_TASK
 *
 * @author DATAMASTER
 * @date 2025-02-11
 */
@Data
@TableName(value = "AST_DISCOVERY_TASK")
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class AssetsDiscoveryTaskDO extends BaseEntity {
    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

    private String name;

    /** id */
    private Long datasourceId;

    private String status;

    /** cron */
    private String cronExpression;

    private String contact;

    private String email;

    /** ID */
    private Long contactId;

    private Long lastTableCount;

    private String contactNumber;

    private String catCode;

    @TableField(exist = false)
    private String catName;

    private String description;

    /** id */
    private Long systemJobId;

    private Date lastExecuteTime;

    @Schema(description = "节点ID", example = "")
    private Long nodeId;

    @Schema(description = "节点编码", example = "")
    private String nodeCode;

    @Schema(description = "任务ID", example = "")
    private Long taskId;

    @Schema(description = "任务编码", example = "")
    private String taskCode;

    private Boolean validFlag;

    @TableLogic
    private Boolean delFlag;
}
