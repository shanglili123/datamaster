package com.datamaster.module.assets.dal.dataobject.assetchild.audit;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import com.datamaster.common.core.domain.BaseEntity;

/**
 *  DO  DA_ASSET_AUDIT_SCHEDULE
 *
 * @author DATAMASTER
 * @date 2025-05-09
 */
@Data
@TableName(value = "AST_ASSET_AUDIT_SCHEDULE")
// 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
// @KeySequence("DA_ASSET_AUDIT_SCHEDULE_seq")
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class AssetsAssetAuditScheduleDO extends BaseEntity {
    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

/** ID */
    private Long assetId;

    /** 01 */
    private String scheduleFlag;

    /** cron */
    private String cronExpression;

    /** id */
    private Long nodeId;

    /**  */
    private String nodeCode;

    /** id */
    private Long taskId;

    /**  */
    private String taskCode;

    /** id */
    private Long systemJobId;

    /**  */
    private Boolean validFlag;

    /**  */
    @TableLogic
    private Boolean delFlag;

}
