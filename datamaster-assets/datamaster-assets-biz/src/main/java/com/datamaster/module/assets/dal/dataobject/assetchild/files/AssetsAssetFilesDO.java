package com.datamaster.module.assets.dal.dataobject.assetchild.files;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import com.datamaster.common.core.domain.BaseEntity;

/**
 * - DO  DA_ASSET_FILES
 *
 * @author DATAMASTER
 * @date 2025-06-26
 */
@Data
@TableName(value = "AST_ASSET_FILES")
// 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
// @KeySequence("DA_ASSET_FILES_seq")
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class AssetsAssetFilesDO extends BaseEntity {
    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

/** id */
    private Long assetId;

    /**  */
    private Integer startColumn;

    /**  */
    private Integer startData;

    /**  */
    private String name;

    /**  */
    private String url;

    /**  */
    private String type;

    /**  */
    private Boolean validFlag;

    /**  */
    @TableLogic
    private Boolean delFlag;

}
