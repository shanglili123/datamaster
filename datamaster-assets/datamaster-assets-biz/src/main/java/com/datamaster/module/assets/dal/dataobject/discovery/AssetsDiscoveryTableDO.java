package com.datamaster.module.assets.dal.dataobject.discovery;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import com.datamaster.common.core.domain.BaseEntity;

/**
 *  DO  DA_DISCOVERY_TABLE
 *
 * @author DATAMASTER
 * @date 2025-02-11
 */
@Data
@TableName(value = "AST_DISCOVERY_TABLE")
// 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
// @KeySequence("DA_DISCOVERY_TABLE_seq")
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class AssetsDiscoveryTableDO extends BaseEntity {
    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

/** id */
    private Long taskId;

    /**  */
    private String tableName;

    /**  */
    private String tableComment;

    /**  */
    private Long dataCount;

    /**  */
    private Long fieldCount;

    /**  */
    private String changeFlag;

    /**  */
    private String status;

    /**  */
    private String ignoreFlag;

    /**  */
    private Boolean validFlag;

    /**  */
    @TableLogic
    private Boolean delFlag;

}
