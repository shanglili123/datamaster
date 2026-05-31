package com.datamaster.module.standards.dal.dataobject.desensitizeList;

import lombok.*;
import lombok.experimental.SuperBuilder;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.datamaster.common.core.domain.BaseEntity;

/**
 * 脱敏清单关联关系 DO 对象 STD_DESENSITIZE_ASSETCOLUMN
 *
 * @author DATAMASTER
 * @date 2026-04-12
 */
@Data
@TableName(value = "STD_DESENSITIZE_ASSETCOLUMN")
// 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
// @KeySequence("STD_DESENSITIZE_ASSETCOLUMN_seq")
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class StandardsDesensitizeAssetcolumnDO extends BaseEntity {
    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

/** 资产ID */
    private Long assetId;

    /** 资产名称 */
    @TableField(exist = false)
    private String assetName;
    /** 资产描述 */
    @TableField(exist = false)
    private String assetDescription;
    /** 资产英文表名 */
    @TableField(exist = false)
    private String assetTableName;
    /** 资产表名 */
    @TableField(exist = false)
    private String assetTableComment;

    /** 资产字段ID */
    private Long assetcolumnId;

    /** 资产字段名称*/
    @TableField(exist = false)
    private String assetcolumnName;
    /** 资产字段描述 */
    @TableField(exist = false)
    private String assetcolumnComment;
    /** 数据分类ID */
    private Long dataCategoryId;
    /** 数据分类名称 */
    @TableField(exist = false)
    private String dataCategoryName;
    /** 数据分级名称 */
    @TableField(exist = false)
    private String dataLevelName;
    /** 脱敏规则名称 */
    @TableField(exist = false)
    private String desensitizeRuleName;

    /** 排序 */
    private Long sortOrder;

    /** 描述 */
    private String description;

    /** 是否有效;0：无效，1：有效 */
    private Boolean validFlag;

    /** 删除标志;1：已删除，0：未删除 */
    @TableLogic
    private Boolean delFlag;


}
