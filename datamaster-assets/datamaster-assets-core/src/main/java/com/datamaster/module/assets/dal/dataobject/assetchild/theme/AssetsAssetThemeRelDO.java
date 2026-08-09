package com.datamaster.module.assets.dal.dataobject.assetchild.theme;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import com.datamaster.common.core.domain.BaseEntity;

/**
 * - DO  DA_ASSET_THEME_REL
 *
 * @author DATAMASTER
 * @date 2025-04-14
 */
@Data
@TableName(value = "AST_ASSET_THEME_REL")
// 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
// @KeySequence("DA_ASSET_THEME_REL_seq")
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class AssetsAssetThemeRelDO extends BaseEntity {
    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

/** id */
    private Long assetId;

    /** id */
    private Long themeId;

    @TableField(exist = false)
    private String themeName;

    /**  */
    private Boolean validFlag;

    /**  */
    @TableLogic
    private Boolean delFlag;

}
