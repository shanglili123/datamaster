package com.datamaster.module.ontology.dal.dataobject;

import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.datamaster.common.core.domain.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * 本体属性 DO
 *
 * 对应表 ONT_PROPERTY
 */
@Data
@TableName("ONT_PROPERTY")
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class PropertyDO extends BaseEntity {

    /**
     * 所属概念ID
     */
    private Long conceptId;

    /**
     * 属性名称
     */
    private String name;

    /**
     * 属性编码
     */
    private String code;

    /**
     * 数据类型：string/integer/decimal/date/boolean/text
     */
    private String dataType;

    /**
     * 描述
     */
    private String description;

    /**
     * 是否主键属性
     */
    private Boolean isPrimary;

    /**
     * 是否必填
     */
    private Boolean isRequired;

    /**
     * 默认值
     */
    private String defaultValue;

    /**
     * 排序
     */
    private Integer sortOrder;

    /**
     * 是否删除：0=未删除 1=已删除
     */
    @TableLogic
    private Integer delFlag;
}
