package com.datamaster.module.standards.dal.dataobject.dataElem;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import com.datamaster.common.core.domain.BaseEntity;

/**
 * 数据元数据规则关联信息 DO 对象 STD_DATA_ELEM_RULE_REL
 *
 * @author DATAMASTER
 * @date 2025-01-21
 */
@Data
@TableName(value = "STD_DATA_ELEM_RULE_REL")
// 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
// @KeySequence("STD_DATA_ELEM_RULE_REL_seq")
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class StandardsDataElemRuleRelDO extends BaseEntity {
    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

private String name;

    /**
     * 状态;0下线1上线
     */
    private String status;

    /**
     * 数据元id
     */
    private Long dataElemId;

    /**
     * 规则类型 1:稽核规则 2:清洗规则
     */
    private String type;

    /**
     * 规则id
     *
     * @see com.datamaster.module.att.dal.dataobject.rule.AttCleanRuleDO#id
     * @see com.datamaster.module.att.dal.dataobject.rule.AttAuditRuleDO#id
     */
    private Long ruleId;

    /**
     * @see com.datamaster.module.att.dal.dataobject.rule.AttCleanRuleDO#code
     * @see com.datamaster.module.att.dal.dataobject.rule.AttAuditRuleDO#code
     */
    private String ruleCode;

    /**
     * @see com.datamaster.module.att.dal.dataobject.rule.AttCleanRuleDO#name
     * @see com.datamaster.module.att.dal.dataobject.rule.AttAuditRuleDO#name
     */
    private String ruleName;

    /**
     * @see com.datamaster.module.att.dal.dataobject.rule.AttAuditRuleDO#qualityDim
     */
    private String dimensionType;

    private String ruleDescription;
    private String errDescription;
    private String suggestion;
    private String whereClause;

    /**
     * 规则配置
     */
    private String rule;

    /**
     * @see com.datamaster.module.att.dal.dataobject.rule.AttAuditRuleDO#strategyKey
     *
     */
    private String ruleType;

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
