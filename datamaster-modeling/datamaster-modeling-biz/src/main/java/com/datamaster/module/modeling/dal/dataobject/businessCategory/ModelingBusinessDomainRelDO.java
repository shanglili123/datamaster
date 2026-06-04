package com.datamaster.module.modeling.dal.dataobject.businessCategory;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import com.datamaster.common.core.domain.BaseEntity;

/**
 * 业务分类数据域关联关系 DO 对象 MDL_BUSINESS_DOMAIN_REL
 *
 * @author DATAMASTER
 * @date 2026-04-12
 */
@Data
@TableName(value = "MDL_BUSINESS_DOMAIN_REL")
// 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
// @KeySequence("MDL_BUSINESS_DOMAIN_REL_seq")
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ModelingBusinessDomainRelDO extends BaseEntity {
    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

/** 业务分类ID */
    private Long businessCategoryId;

    /** 数据域ID */
    private Long dataDomainId;

    /** 业务分类名称 */
    private String businessCategoryName;

    /** 数据域名称 */
    private String dataDomainName;

    /** 排序 */
    private Long sortOrder;

    /** 描述 */
    private String description;

    /** 项目ID */
    private Long projectId;

    /** 项目编码 */
    private String projectCode;

    /** 是否有效;0：无效，1：有效 */
    private Boolean validFlag;

    /** 删除标志;1：已删除，0：未删除 */
    @TableLogic
    private Boolean delFlag;


}
