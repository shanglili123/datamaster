package com.datamaster.module.assets.dal.dataobject.sensitiveLevel;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import com.datamaster.common.core.domain.BaseEntity;

/**
 *  DO  DA_SENSITIVE_LEVEL
 *
 * @author DATAMASTER
 * @date 2025-01-21
 */
@Data
@TableName(value = "AST_SENSITIVE_LEVEL")
// 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
// @KeySequence("DA_SENSITIVE_LEVEL_seq")
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class AssetsSensitiveLevelDO extends BaseEntity {
    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

/**  */
    private String sensitiveLevel;

    /**  */
    private String sensitiveRule;

    /**  */
    private Long startCharLoc;

    /**  */
    private Long endCharLoc;

    /**  */
    private String maskCharacter;

    /**  */
    private String onlineFlag;

    /**  */
    private String description;

    /**  */
    private Boolean validFlag;

    /**  */
    @TableLogic
    private Boolean delFlag;

}
