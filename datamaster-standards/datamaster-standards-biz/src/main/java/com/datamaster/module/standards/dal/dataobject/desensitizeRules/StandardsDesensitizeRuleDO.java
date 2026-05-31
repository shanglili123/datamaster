package com.datamaster.module.standards.dal.dataobject.desensitizeRules;

import lombok.*;
import lombok.experimental.SuperBuilder;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.datamaster.common.core.domain.BaseEntity;

import java.util.List;

/**
 * 脱敏规则 DO 对象 STD_DESENSITIZE_RULE
 *
 * @author DATAMASTER
 * @date 2026-04-10
 */
@Data
@TableName(value = "STD_DESENSITIZE_RULE")
// 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
// @KeySequence("STD_DESENSITIZE_RULE_seq")
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class StandardsDesensitizeRuleDO extends BaseEntity {
    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

/** 分级名称 */
    private String name;

    /** 数据分类ID */
    private Long dataCategoryId;
    /** 数据分类名称 */
    @TableField(exist = false)
    private String dataCategoryName;

    /** 应用场景;1：数据资产  2：数据查询  3：数据服务 */
    private String applicationScene;

    /** 脱敏方式;1：底层脱敏  2：展示脱敏 */
    private String maskType;

    /** 替换规则 */
    private String replaceRule;

    /** 替换内容 */
    private String replaceContent;
    /** 区间集合 */
    @TableField(exist = false)
    private List<StandardsDesensitizeIntervalDO> intervalList;

    /** 排序 */
    private Long sortOrder;

    /** 描述 */
    private String description;

    /** 是否有效;0：无效，1：有效 */
    private Boolean validFlag;

    /** 删除标志;1：已删除，0：未删除 */
    @TableLogic
    private Boolean delFlag;


}
