package com.datamaster.module.assets.dal.dataobject.discovery;import com.baomidou.mybatisplus.annotation.*;import io.swagger.v3.oas.annotations.media.Schema;import lombok.*;
import lombok.experimental.SuperBuilder;
import com.datamaster.common.core.domain.BaseEntity;import java.util.Date;
/** *  DO  DA_DISCOVERY_TASK * * @author DATAMASTER * @date 2025-02-11 */

@Data
@TableName(value = "AST_DISCOVERY_TASK")
// 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
// @KeySequence("DA_DISCOVERY_TASK_seq")
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)public class AssetsDiscoveryTaskDO extends BaseEntity {    @TableField(exist = false)    private static final long serialVersionUID = 1L;/**  */
    private String name;    /** id */
    private Long datasourceId;    /**  */
    private String status;    /** cron */
    private String cronExpression;    /**  */
    private String contact;    /**  */
    private String email;    /** ID */
    private Long contactId;    /**  */
    private Long lastTableCount;    /**  */
    private String contactNumber;    /**  */
    private String catCode;    @TableField(exist = false)    private String catName;    /**  */
    private String description;    /** id */
    private Long systemJobId;    /**  */
    private Date lastExecuteTime;    /** id */

@Schema(description = "id", example = "")    private Long nodeId;    /**  */

@Schema(description = "", example = "")    private String nodeCode;    /** id */

@Schema(description = "id", example = "")    private Long taskId;    /**  */

@Schema(description = "", example = "")    private String taskCode;    /**  */
    private Boolean validFlag;    /**  */

@TableLogic    private Boolean delFlag;
}