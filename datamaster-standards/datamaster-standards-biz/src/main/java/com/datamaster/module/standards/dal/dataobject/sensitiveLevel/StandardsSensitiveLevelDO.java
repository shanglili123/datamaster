package com.datamaster.module.standards.dal.dataobject.sensitiveLevel;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import com.datamaster.common.core.domain.BaseEntity;

/**
 * 敏感等级 DO 对象 STD_SENSITIVE_LEVEL
 *
 * @author Chaos
 * @date 2025-01-21
 */
@Data
@TableName(value = "STD_SENSITIVE_LEVEL")
// 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
// @KeySequence("STD_SENSITIVE_LEVEL_seq")
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class StandardsSensitiveLevelDO extends BaseEntity {
    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

/**
     * 敏感级别
     */
    private String sensitiveLevel;

    /**
     * 敏感规则
     */
    private String sensitiveRule;

    /**
     * 起始字符位置
     */
    private Long startCharLoc;

    /**
     * 截止字符位置
     */
    private Long endCharLoc;

    /**
     * 遮盖字符
     */
    private String maskCharacter;

    /**
     * 上下线标识
     * 0：下线，1：上线
     */
    private String onlineFlag;

    /**
     * 描述
     */
    private String description;

    /** 项目ID */
    private Long projectId;

    /** 项目编码 */
    private String projectCode;

    /**
     * 是否有效
     */
    private Boolean validFlag;

    /**
     * 删除标志
     */
    @TableLogic
    private Boolean delFlag;

}
