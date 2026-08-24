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
 * 本体关系 DO
 *
 * 对应表 ONT_RELATION
 */
@Data
@TableName("ONT_RELATION")
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class RelationDO extends BaseEntity {

    /**
     * 所属本体ID
     */
    private Long ontologyId;

    /**
     * 关系名称
     */
    private String name;

    /**
     * 关系编码
     */
    private String code;

    /**
     * 源概念ID
     */
    private Long sourceConceptId;

    /**
     * 目标概念ID
     */
    private Long targetConceptId;

    /**
     * 关系类型：one_to_one/one_to_many/many_to_one/many_to_many
     */
    private String relationType;

    /**
     * 描述
     */
    private String description;

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
