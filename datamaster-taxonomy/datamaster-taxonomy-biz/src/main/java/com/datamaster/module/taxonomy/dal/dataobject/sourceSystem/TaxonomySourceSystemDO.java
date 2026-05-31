package com.datamaster.module.taxonomy.dal.dataobject.sourceSystem;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import com.datamaster.common.core.domain.BaseEntity;

/**
 * 来源系统 DO 对象 TAX_SOURCE_SYSTEM
 *
 * @author DATAMASTER
 * @date 2026-04-03
 */
@Data
@TableName(value = "TAX_SOURCE_SYSTEM")
// 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
// @KeySequence("TAX_SOURCE_SYSTEM_seq")
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class TaxonomySourceSystemDO extends BaseEntity {
    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

/** 系统名称 */
    private String name;

    /** 系统类型 */
    private String type;

    /** 排序 */
    private Long sortOrder;

    /** 描述 */
    private String description;

    /** 是否有效;0：无效，1：有效 */
    private Boolean validFlag;

    /** 负责人 */
    private String responsiblePerson;

    /** 负责人名称 */
    @TableField(exist = false)
    private String responsiblePersonName;

    /** 对接人 */
    private String contactPerson;
    /** 对接人名称 */
    @TableField(exist = false)
    private String contactPersonName;


    /** 删除标志;1：已删除，0：未删除 */
    @TableLogic
    private Boolean delFlag;


}
