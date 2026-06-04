package com.datamaster.module.taxonomy.dal.dataobject.theme;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;
import lombok.experimental.SuperBuilder;
import com.datamaster.common.core.domain.BaseEntity;

/**
 * 主题 DO 对象 TAX_THEME
 *
 * @author DATAMASTER
 * @date 2025-01-20
 */
@Data
@TableName(value = "TAX_THEME")
// 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
// @KeySequence("TAX_THEME_seq")
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class TaxonomyThemeDO extends BaseEntity {
    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

/** 主题名称 */
    private String name;

    /** 图标url */
    private String icon;

    /** 排序 */
    private Long sortOrder;

    /** 描述 */
    private String description;

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
