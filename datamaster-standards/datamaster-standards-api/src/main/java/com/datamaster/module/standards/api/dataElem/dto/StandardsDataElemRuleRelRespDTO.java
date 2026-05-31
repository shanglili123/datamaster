

package com.datamaster.module.standards.api.dataElem.dto;

import lombok.Data;

/**
 * 数据元数据规则关联信息 DTO 对象 STD_DATA_ELEM_RULE_REL
 *
 * @author DATAMASTER
 * @date 2025-01-21
 */
@Data
public class StandardsDataElemRuleRelRespDTO {

    private static final long serialVersionUID = 1L;


    /**
     * ID
     */
    private Long id;

    private String name;

    /**
     * 状态;0下线1上线
     */
    private Integer status;

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
     */
    private String ruleType;

    /**
     * 是否有效
     */
    private Boolean validFlag;

    /**
     * 删除标志
     */
    private Boolean delFlag;

}
