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
 * 本体概念 DO
 *
 * 对应表 ONT_CONCEPT
 */
@Data
@TableName("ONT_CONCEPT")
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ConceptDO extends BaseEntity {

    /**
     * 所属本体ID
     */
    private Long ontologyId;

    /**
     * 概念名称
     */
    private String name;

    /**
     * 概念编码
     */
    private String code;

    /**
     * 描述
     */
    private String description;

    /**
     * 图标标识（可视化用）
     */
    private String icon;

    /**
     * 颜色（可视化用）
     */
    private String color;

    /**
     * 排序
     */
    private Integer sortOrder;

    /**
     * 状态：0=草稿 1=已发布
     */
    private Integer status;

    /**
     * 是否删除：0=未删除 1=已删除
     */
    @TableLogic
    private Integer delFlag;
}
