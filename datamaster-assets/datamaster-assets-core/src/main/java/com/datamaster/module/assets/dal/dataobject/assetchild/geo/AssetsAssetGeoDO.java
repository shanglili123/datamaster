package com.datamaster.module.assets.dal.dataobject.assetchild.geo;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.SuperBuilder;
import com.datamaster.common.core.domain.BaseEntity;

/**
 * - DO  DA_ASSET_GEO
 *
 * @author DATAMASTER
 * @date 2025-04-14
 */
@Data
@TableName(value = "AST_ASSET_GEO")
// 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
// @KeySequence("DA_ASSET_GEO_seq")
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class AssetsAssetGeoDO extends BaseEntity {
    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

/** id */
    private Long assetId;

    /**  */
    private String fileName;

    /**  */
    private String fileUrl;

    /**  */
    private String elementType;

    /**  */
    private String coordinateSystem;

    @Schema(description = "", example = "")
    private String fileType;

    /**  */
    private Boolean validFlag;

    /**  */
    @TableLogic
    private Boolean delFlag;

}
