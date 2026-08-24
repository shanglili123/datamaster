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
 * 本体 DO
 *
 * 对应表 ONT_ONTOLOGY
 */
@Data
@TableName("ONT_ONTOLOGY")
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class OntologyDO extends BaseEntity {

    /**
     * 本体名称
     */
    private String name;

    /**
     * 本体编码
     */
    private String code;

    /**
     * 描述
     */
    private String description;

    /**
     * 所属空间ID
     */
    private Long spaceId;

    /**
     * 空间编码
     */
    private String spaceCode;

    /**
     * 状态：0=草稿 1=已发布 2=已归档
     */
    private Integer status;

    /**
     * 是否删除：0=未删除 1=已删除
     */
    @TableLogic
    private Integer delFlag;
}
