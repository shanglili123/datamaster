package com.datamaster.metadata.dal.dataobject.discovery;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;
import lombok.experimental.SuperBuilder;
import com.datamaster.common.core.domain.BaseEntity;

import java.util.Date;

/**
 *  DO  AST_DISCOVERY_TASK_LOG
 *
 * @author DATAMASTER
 * @date 2025-02-17
 */
@Data
@TableName(value = "AST_DISCOVERY_TASK_LOG")
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class AssetsDiscoveryTaskLogDO extends BaseEntity {
    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

    private String name;

    /** id */
    private Long nodeId;

    private String nodeCode;

    private String taskName;

    /** id */
    private Long taskId;

    private String taskCode;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date startTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date endTime;

    private String status;

    private Long newTableCount;

    private Long modifiedTableCount;

    private Long deletedTableCount;

    private String contact;

    /** ID */
    private Long contactId;

    private String contactNumber;

    private String email;

    /** DolphinScheduler id */
    private Long dsId;

    /** DolphinScheduler id */
    private Long dsTaskInstanceId;

    private String path;

    private Boolean validFlag;

    @TableLogic
    private Boolean delFlag;
}
