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
 * 属性字段绑定 DO
 *
 * 对应表 ONT_PROPERTY_COLUMN
 */
@Data
@TableName("ONT_PROPERTY_COLUMN")
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class PropertyColumnDO extends BaseEntity {

    /** 属性ID */
    private Long propertyId;

    /** 概念表绑定ID */
    private Long conceptTableId;

    /** 物理列名 */
    private String columnName;

    @TableLogic
    private Integer delFlag;
}
