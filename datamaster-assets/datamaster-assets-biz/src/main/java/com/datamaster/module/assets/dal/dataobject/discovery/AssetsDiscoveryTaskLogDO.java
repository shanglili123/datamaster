package com.datamaster.module.assets.dal.dataobject.discovery;import com.baomidou.mybatisplus.annotation.*;import com.fasterxml.jackson.annotation.JsonFormat;import lombok.*;
import lombok.experimental.SuperBuilder;
import com.datamaster.common.core.domain.BaseEntity;import java.util.Date;
/** *  DO  DA_DISCOVERY_TASK_LOG * * @author DATAMASTER * @date 2025-02-17 */

@Data
@TableName(value = "AST_DISCOVERY_TASK_LOG")
// 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
// @KeySequence("DA_DISCOVERY_TASK_LOG_seq")
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)public class AssetsDiscoveryTaskLogDO extends BaseEntity {    @TableField(exist = false)    private static final long serialVersionUID = 1L;/**  */
    private String name;    /** id */
    private Long nodeId;    /**  */
    private String nodeCode;    /**  */
    private String taskName;    /** id */
    private Long taskId;    /**  */
    private String taskCode;    /**  */

@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")    private Date startTime;    /**  */

@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")    private Date endTime;    /**  */
    private String status;    /**  */
    private Long newTableCount;    /**  */
    private Long modifiedTableCount;    /**  */
    private Long deletedTableCount;    /**  */
    private String contact;    /** ID */
    private Long contactId;    /**  */
    private String contactNumber;    /**  */
    private String email;    /** DolphinSchedulerid */
    private Long dsId;    /** DolphinSchedulerid */
    private Long dsTaskInstanceId;    /**  */
    private String path;    /**  */
    private Boolean validFlag;    /**  */

@TableLogic    private Boolean delFlag;
}