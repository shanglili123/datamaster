package com.datamaster.module.taxonomy.dal.dataobject.cat;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import com.datamaster.common.core.domain.BaseEntity;

/**
 * 标准信息分类管理 DO 对象 TAX_DOCUMENT_CAT
 *
 * @author DATAMASTER
 * @date 2025-08-21
 */
@Data
@TableName(value = "TAX_DOCUMENT_CAT")
// 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
// @KeySequence("TAX_DOCUMENT_CAT_seq")
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class TaxonomyDocumentCatDO extends BaseEntity {
    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

/** 类别名称 */
    private String name;

    /** 关联上级ID */
    private Long parentId;

    /** 类别排序 */
    private Long sortOrder;

    /** 描述 */
    private String description;

    /** 层级编码 */
    private String code;

    /** 项目ID */
    private Long projectId;

    /** 项目编码 */
    private String projectCode;

    /** 是否有效 */
    private Boolean validFlag;

    /** 删除标志 */
    @TableLogic
    private Boolean delFlag;


}
