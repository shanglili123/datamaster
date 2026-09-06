package com.datamaster.module.ontology.api.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 本体属性语义 DTO — AI 问数/Skill 生成专用
 *
 * 对应 ONT_PROPERTY 与 ONT_PROPERTY_COLUMN 的关联视图：
 * 一个属性（业务语义）映射到概念表上的一个物理列。
 */
@Data
public class SemanticPropertyDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 属性ID（ONT_PROPERTY.id） */
    private Long propertyId;

    /** 属性业务名称（ONT_PROPERTY.name） */
    private String propertyName;

    /** 属性编码（ONT_PROPERTY.code） */
    private String propertyCode;

    /** 属性数据类型（string/integer/decimal/date/boolean/text） */
    private String dataType;

    /** 属性描述（ONT_PROPERTY.description） */
    private String propertyDescription;

    /** 是否主键属性 */
    private Boolean isPrimary;

    /** 对应物理列名（ONT_PROPERTY_COLUMN.column_name） */
    private String physicalColumnName;
}
