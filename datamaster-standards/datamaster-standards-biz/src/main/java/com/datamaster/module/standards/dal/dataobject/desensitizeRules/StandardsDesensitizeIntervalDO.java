package com.datamaster.module.standards.dal.dataobject.desensitizeRules;

import lombok.*;
import lombok.experimental.SuperBuilder;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.datamaster.common.core.domain.BaseEntity;

/**
 * 脱敏区间 DO 对象 STD_DESENSITIZE_INTERVAL
 *
 * @author DATAMASTER
 * @date 2026-04-10
 */
@Data
@TableName(value = "STD_DESENSITIZE_INTERVAL")
// 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
// @KeySequence("STD_DESENSITIZE_INTERVAL_seq")
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class StandardsDesensitizeIntervalDO extends BaseEntity {
    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

/** 脱敏规则ID */
    private Long desensitizeRuleId;

    /** 区间号 */
    private Long intervalNo;

    /** 起始值 */
    private Long startNum;

    /** 末尾值 */
    private Long endNum;

    /** 是否有效;0：无效，1：有效 */
    private Boolean validFlag;

    /** 删除标志;1：已删除，0：未删除 */
    @TableLogic
    private Boolean delFlag;


}
