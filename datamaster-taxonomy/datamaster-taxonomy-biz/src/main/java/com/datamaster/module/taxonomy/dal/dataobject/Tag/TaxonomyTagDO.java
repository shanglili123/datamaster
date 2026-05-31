package com.datamaster.module.taxonomy.dal.dataobject.Tag;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import com.datamaster.common.core.domain.BaseEntity;

/**
 * 标签管理 DO 对象 TAX_TAG
 *
 * @author DATAMASTER
 * @date 2025-07-11
 */
@Data
@TableName(value = "TAX_TAG")
// 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
// @KeySequence("TAX_TAG_seq")
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class TaxonomyTagDO extends BaseEntity {
    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

/** 名称 */
    private String name;

    /** 描述 */
    private String description;

    /** 类目编码 */
    private String catCode;

    /** 类目名称 */
    private String catName;

    /** 资产数量 */
    private Long aeestCount;

    /** 状态 */
    private String status;

    /** 扩展信息别名 */
    private String alias;

    /** 近义词 */
    private String nearSynonyms;

    /** 同义词 */
    private String synonyms;

    /** 是否有效 */
    private Boolean validFlag;

    /** 删除标志 */
    @TableLogic
    private Boolean delFlag;


}
