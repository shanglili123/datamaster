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
 * 关系字段绑定 DO
 *
 * 对应表 ONT_RELATION_COLUMN
 */
@Data
@TableName("ONT_RELATION_COLUMN")
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class RelationColumnDO extends BaseEntity {

    /** 关系ID */
    private Long relationId;

    /** 源概念表绑定ID */
    private Long sourceConceptTableId;

    /** 源物理列名 */
    private String sourceColumn;

    /** 目标概念表绑定ID */
    private Long targetConceptTableId;

    /** 目标物理列名 */
    private String targetColumn;

    @TableLogic
    private Integer delFlag;
}
